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
            Log.d(TAG, "开始导出验证过的数据...")
            
            val journeys = labelingDao.getVerifiedJourneysForExport(limit = 10000)
            if (journeys.isEmpty()) {
                Log.w(TAG, "没有验证过的数据可导出")
                return@withContext 0
            }
            
            Log.d(TAG, "找到 ${journeys.size} 条验证过的记录")
            
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
                            Log.d(TAG, "已处理 $processedCount 条记录...")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "处理记录 ${journey.id} 失败: ${e.message}")
                    }
                }

                writer.flush()
            }

            val fileSize = file.length() / 1024  // KB
            Log.d(TAG, "成功导出 $journeys.size 条记录 (${fileSize}KB) -> $outputFile")

            journeys.size
        } catch (e: Exception) {
            Log.e(TAG, "导出CSV失败: ${e.message}", e)
            -1
        }
    }

    /**
     * 导出所有数据（包括未验证的自动标记数据）用于预览
     */
    suspend fun exportAllDataToCSV(outputFile: String): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始导出所有数据...")

            val journeys = labelingDao.getJourneysForExport(limit = 10000)
            if (journeys.isEmpty()) {
                Log.w(TAG, "没有数据可导出")
                return@withContext 0
            }

            Log.d(TAG, "找到 ${journeys.size} 条记录")

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
                        Log.e(TAG, "处理记录 ${journey.id} 失败: ${e.message}")
                    }
                }
                
                writer.flush()
            }
            
            val fileSize = file.length() / 1024
            Log.d(TAG, "成功导出 $journeys.size 条记录 (${fileSize}KB) -> $outputFile")
            
            journeys.size
        } catch (e: Exception) {
            Log.e(TAG, "导出CSV失败: ${e.message}", e)
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
            report.append("=== 训练数据统计报告 ===\n")
            report.append("生成时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())}\n\n")
            
            report.append("总体统计:\n")
            report.append("- 总记录数: $totalCount\n")
            report.append("- 已验证: $verifiedCount (${if (totalCount > 0) verifiedCount * 100 / totalCount else 0}%)\n")
            report.append("- 待验证: ${totalCount - verifiedCount}\n\n")
            
            report.append("交通方式分布:\n")
            modeDistribution.forEach { (mode, count) ->
                val percentage = if (totalCount > 0) count * 100 / totalCount else 0
                report.append("- $mode: $count 个 ($percentage%)\n")
            }
            report.append("\n")
            
            report.append("标签来源分布:\n")
            sourceDistribution.forEach { (source, count) ->
                val percentage = if (totalCount > 0) count * 100 / totalCount else 0
                report.append("- $source: $count 个 ($percentage%)\n")
            }
            report.append("\n")
            
            // 数据质量评估
            report.append("数据质量评估:\n")
            if (totalCount < 100) {
                report.append("⚠️  数据量较小 (< 100条)，建议继续收集\n")
            } else if (totalCount < 500) {
                report.append("⚠️  数据量适中 (< 500条)，可以开始训练初版模型\n")
            } else {
                report.append("✓ 数据量充足 (≥ 500条)，适合训练生产级模型\n")
            }
            
            if (verifiedCount < totalCount * 0.8) {
                report.append("⚠️  验证率较低 (< 80%)，建议增加人工验证\n")
            } else {
                report.append("✓ 验证率高 (≥ 80%)，数据质量良好\n")
            }
            
            // 检查各交通方式是否均衡
            val counts = modeDistribution.map { it.count }
            if (counts.isNotEmpty()) {
                val minCount = counts.minOrNull() ?: 0
                val maxCount = counts.maxOrNull() ?: 0
                val ratio = if (minCount > 0) maxCount / minCount else 0
                
                if (ratio > 5) {
                    report.append("⚠️  交通方式分布不均衡 (最大/最小比: $ratio)，可能影响模型性能\n")
                } else {
                    report.append("✓ 交通方式分布相对均衡\n")
                }
            }
            
            report.toString()
        } catch (e: Exception) {
            Log.e(TAG, "生成报告失败: ${e.message}", e)
            "报告生成失败: ${e.message}"
        }
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
