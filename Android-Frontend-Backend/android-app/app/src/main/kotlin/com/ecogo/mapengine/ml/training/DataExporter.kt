package com.ecogo.mapengine.ml.training

import android.util.Log
import com.ecogo.mapengine.ml.database.ActivityLabelingDao
import com.ecogo.mapengine.ml.model.JourneyCSVRecord
import com.ecogo.mapengine.ml.model.JourneyFeatures
import com.ecogo.mapengine.ml.model.LabeledJourney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据导出工具 - 将标记的出行数据导出为CSV格式供Python训练脚本使用
 *
 * CSV格式：
 * journeyId,timestamp,transportMode,labelSource,avgSpeed,maxSpeed,speedVariance,
 * accelMagnitude,heightVariance,gpsAccuracy,motorwayPercentage,cyclewayPercentage,
 * journeyDuration,isVerified
 */
class DataExporter(
    private val labelingDao: ActivityLabelingDao,
    private val featureExtractor: FeatureExtractor
) {
    
    companion object {
        private const val TAG = "DataExporter"
        private const val CSV_HEADER =
            "journeyId,timestamp,transportMode,labelSource," +
            "accelMeanX,accelMeanY,accelMeanZ,accelStdX,accelStdY,accelStdZ,accelMagnitude," +
            "gyroMeanX,gyroMeanY,gyroMeanZ,gyroStdX,gyroStdY,gyroStdZ," +
            "journeyDuration," +
            "gpsSpeedMean,gpsSpeedStd,gpsSpeedMax," +
            "isVerified\n"
    }
    
    /**
     * 导出所有验证过的出行数据为CSV
     * 仅导出经过人工验证的数据，确保标签质量
     *
     * @param outputFile 输出文件路径
     * @return 导出的记录数，或-1如果出错
     */
    suspend fun exportVerifiedDataToCSV(outputFile: String): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting export of verified data...")
            
            val journeys = labelingDao.getVerifiedJourneysForExport(limit = 10000)
            if (journeys.isEmpty()) {
                Log.w(TAG, "No verified data to export")
                return@withContext 0
            }
            
            Log.d(TAG, "Found ${journeys.size} verified records")
            
            // 创建输出文件并写入CSV头
            val file = File(outputFile)
            file.parentFile?.mkdirs()
            
            FileWriter(file).use { writer ->
                writer.write(CSV_HEADER)
                
                var processedCount = 0
                journeys.forEach { journey ->
                    try {
                        val features = featureExtractor.extractFeatures(journey)
                        val record = featuresToCSVRecord(journey, features)

                        writer.append(recordToCSVLine(record))
                        processedCount++

                        // 每100条记录打印一次进度
                        if (processedCount % 100 == 0) {
                            Log.d(TAG, "Processed $processedCount records...")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process record ${journey.id}: ${e.message}")
                    }
                }

                writer.flush()
            }

            val fileSize = file.length() / 1024  // KB
            Log.d(TAG, "Successfully exported $journeys.size records (${fileSize}KB) -> $outputFile")

            journeys.size
        } catch (e: Exception) {
            Log.e(TAG, "CSV export failed: ${e.message}", e)
            -1
        }
    }

    /**
     * 导出所有数据（包括未验证的自动标记数据）用于预览
     */
    suspend fun exportAllDataToCSV(outputFile: String): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting export of all data...")

            val journeys = labelingDao.getJourneysForExport(limit = 10000)
            if (journeys.isEmpty()) {
                Log.w(TAG, "No data to export")
                return@withContext 0
            }

            Log.d(TAG, "Found ${journeys.size} records")

            val file = File(outputFile)
            file.parentFile?.mkdirs()

            FileWriter(file).use { writer ->
                writer.write(CSV_HEADER)

                var processedCount = 0
                journeys.forEach { journey ->
                    try {
                        val features = featureExtractor.extractFeatures(journey)
                        val record = featuresToCSVRecord(journey, features)

                        writer.append(recordToCSVLine(record))
                        processedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process record ${journey.id}: ${e.message}")
                    }
                }
                
                writer.flush()
            }
            
            val fileSize = file.length() / 1024
            Log.d(TAG, "Successfully exported $journeys.size records (${fileSize}KB) -> $outputFile")
            
            journeys.size
        } catch (e: Exception) {
            Log.e(TAG, "CSV export failed: ${e.message}", e)
            -1
        }
    }
    
    /**
     * 生成数据统计报告
     */
    suspend fun generateDataReport(): String = withContext(Dispatchers.Default) {
        try {
            val totalCount = labelingDao.getTotalCount()
            val verifiedCount = labelingDao.getVerifiedCount()
            val modeDistribution = labelingDao.getTransportModeDistribution()
            val sourceDistribution = labelingDao.getLabelSourceDistribution()

            val report = StringBuilder()
            report.append("=== Training Data Statistics Report ===\n")
            report.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n\n")

            report.append("Overall Statistics:\n")
            report.append("- Total records: $totalCount\n")
            report.append("- Verified: $verifiedCount (${if (totalCount > 0) verifiedCount * 100 / totalCount else 0}%)\n")
            report.append("- Pending verification: ${totalCount - verifiedCount}\n\n")

            appendDistribution(report, "Transport Mode Distribution", modeDistribution.map { it.transportMode to it.count }, totalCount)
            appendDistribution(report, "Label Source Distribution", sourceDistribution.map { it.labelSource to it.count }, totalCount)

            appendQualityAssessment(report, totalCount, verifiedCount, modeDistribution)

            report.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Report generation failed: ${e.message}", e)
            "Report generation failed: ${e.message}"
        }
    }

    private fun appendDistribution(
        report: StringBuilder,
        title: String,
        distribution: List<Pair<String, Int>>,
        totalCount: Int
    ) {
        report.append("$title:\n")
        distribution.forEach { (label, count) ->
            val percentage = if (totalCount > 0) count * 100 / totalCount else 0
            report.append("- $label: $count ($percentage%)\n")
        }
        report.append("\n")
    }

    private fun appendQualityAssessment(
        report: StringBuilder,
        totalCount: Int,
        verifiedCount: Int,
        modeDistribution: List<com.ecogo.mapengine.ml.database.TransportModeCount>
    ) {
        report.append("Data Quality Assessment:\n")
        report.append(assessDataVolume(totalCount))
        report.append(assessVerificationRate(totalCount, verifiedCount))
        report.append(assessModeBalance(modeDistribution))
    }

    private fun assessDataVolume(totalCount: Int): String = when {
        totalCount < 100 -> "⚠️  Small data volume (< 100 records), recommend collecting more\n"
        totalCount < 500 -> "⚠️  Moderate data volume (< 500 records), can start training initial model\n"
        else -> "✓ Sufficient data volume (≥ 500 records), suitable for production model training\n"
    }

    private fun assessVerificationRate(totalCount: Int, verifiedCount: Int): String =
        if (verifiedCount < totalCount * 0.8) "⚠️  Low verification rate (< 80%), recommend more manual verification\n"
        else "✓ High verification rate (≥ 80%), good data quality\n"

    private fun assessModeBalance(modeDistribution: List<com.ecogo.mapengine.ml.database.TransportModeCount>): String {
        val counts = modeDistribution.map { it.count }
        if (counts.isEmpty()) return ""
        val minCount = counts.minOrNull() ?: 0
        val maxCount = counts.maxOrNull() ?: 0
        val ratio = if (minCount > 0) maxCount / minCount else 0
        return if (ratio > 5) "⚠️  Unbalanced transport mode distribution (max/min ratio: $ratio), may affect model performance\n"
        else "✓ Relatively balanced transport mode distribution\n"
    }
    
    /**
     * 从JourneyFeatures构建CSV记录
     */
    private fun featuresToCSVRecord(journey: LabeledJourney, features: JourneyFeatures): JourneyCSVRecord {
        return JourneyCSVRecord(
            journeyId = journey.id,
            timestamp = journey.startTime,
            transportMode = journey.transportMode,
            labelSource = journey.labelSource,
            accelMeanX = features.accelMeanX,
            accelMeanY = features.accelMeanY,
            accelMeanZ = features.accelMeanZ,
            accelStdX = features.accelStdX,
            accelStdY = features.accelStdY,
            accelStdZ = features.accelStdZ,
            accelMagnitude = features.accelMagnitude,
            gyroMeanX = features.gyroMeanX,
            gyroMeanY = features.gyroMeanY,
            gyroMeanZ = features.gyroMeanZ,
            gyroStdX = features.gyroStdX,
            gyroStdY = features.gyroStdY,
            gyroStdZ = features.gyroStdZ,
            journeyDuration = features.journeyDuration,
            gpsSpeedMean = features.gpsSpeedMean,
            gpsSpeedStd = features.gpsSpeedStd,
            gpsSpeedMax = features.gpsSpeedMax,
            isVerified = journey.isVerified
        )
    }

    /**
     * 将记录转换为CSV行
     */
    private fun recordToCSVLine(record: JourneyCSVRecord): String {
        return "${record.journeyId}," +
                "${record.timestamp}," +
                "${record.transportMode}," +
                "${record.labelSource}," +
                "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,".format(
                    record.accelMeanX, record.accelMeanY, record.accelMeanZ,
                    record.accelStdX, record.accelStdY, record.accelStdZ,
                    record.accelMagnitude
                ) +
                "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,".format(
                    record.gyroMeanX, record.gyroMeanY, record.gyroMeanZ,
                    record.gyroStdX, record.gyroStdY, record.gyroStdZ
                ) +
                "%.2f,".format(record.journeyDuration) +
                "%.4f,%.4f,%.4f,".format(
                    record.gpsSpeedMean, record.gpsSpeedStd, record.gpsSpeedMax
                ) +
                "${if (record.isVerified) 1 else 0}\n"
    }
}
