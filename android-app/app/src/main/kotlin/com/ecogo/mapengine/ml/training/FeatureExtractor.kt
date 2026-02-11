package com.ecogo.mapengine.ml.training

import android.util.Log
import com.ecogo.mapengine.ml.model.JourneyFeatures
import com.ecogo.mapengine.ml.model.LabeledJourney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * 特征提取工具 - 从原始数据提取用于ML训练的特征向量
 *
 * 输入：LabeledJourney对象（包含传感器数据）
 * 输出：17维特征向量 + 目标标签（交通方式）
 *
 * 特征类别：
 * 1. 加速度计特征 (7个)
 * 2. 陀螺仪特征 (6个)
 * 3. 时间特征 (1个)
 * 4. GPS速度特征 (3个)
 * 总计：17个特征 + 目标标签
 */
class FeatureExtractor {

    companion object {
        private const val TAG = "FeatureExtractor"
    }

    /**
     * 从LabeledJourney提取特征向量
     */
    suspend fun extractFeatures(journey: LabeledJourney): JourneyFeatures = withContext(Dispatchers.Default) {
        try {
            // 解析传感器数据
            val accelData = parseSensorData(journey.accelerometerData)
            val gyroData = parseSensorData(journey.gyroscopeData)

            // 提取加速度计特征
            val (accelMeanX, accelMeanY, accelMeanZ, accelStdX, accelStdY, accelStdZ, accelMagnitude) =
                extractAccelerometerFeatures(accelData)

            // 提取陀螺仪特征
            val (gyroMeanX, gyroMeanY, gyroMeanZ, gyroStdX, gyroStdY, gyroStdZ) =
                extractGyroscopeFeatures(gyroData)

            // 提取时间特征
            val journeyDuration = (journey.endTime - journey.startTime) / 1000f // 秒

            // 提取GPS速度特征（从LabeledJourney的速度统计字段）
            val gpsSpeedMean = journey.avgSpeed
            val gpsSpeedStd = sqrt(journey.speedVariance)
            val gpsSpeedMax = journey.maxSpeed

            JourneyFeatures(
                // 加速度计特征
                accelMeanX = accelMeanX,
                accelMeanY = accelMeanY,
                accelMeanZ = accelMeanZ,
                accelStdX = accelStdX,
                accelStdY = accelStdY,
                accelStdZ = accelStdZ,
                accelMagnitude = accelMagnitude,

                // 陀螺仪特征
                gyroMeanX = gyroMeanX,
                gyroMeanY = gyroMeanY,
                gyroMeanZ = gyroMeanZ,
                gyroStdX = gyroStdX,
                gyroStdY = gyroStdY,
                gyroStdZ = gyroStdZ,

                // 时间特征
                journeyDuration = journeyDuration,

                // GPS速度特征
                gpsSpeedMean = gpsSpeedMean,
                gpsSpeedStd = gpsSpeedStd,
                gpsSpeedMax = gpsSpeedMax,

                // 目标标签
                transportMode = journey.transportMode
            )
        } catch (e: Exception) {
            Log.e(TAG, "特征提取失败: ${e.message}", e)
            throw e
        }
    }

    /**
     * 解析JSON格式的传感器数据
     * 格式: [{"x":0.5,"y":0.3,"z":9.8},...]
     */
    private fun parseSensorData(jsonData: String): List<Triple<Float, Float, Float>> {
        return try {
            // 简化解析：使用正则表达式提取x,y,z值
            val pattern = """"([xyz])":([0-9.\-E+]+)""".toRegex()
            val matches = pattern.findAll(jsonData)

            val values = mutableMapOf<String, MutableList<Float>>()
            matches.forEach { match ->
                val axis = match.groupValues[1]
                val value = match.groupValues[2].toFloat()
                values.computeIfAbsent(axis) { mutableListOf() }.add(value)
            }

            if (values["x"].isNullOrEmpty() || values["y"].isNullOrEmpty() || values["z"].isNullOrEmpty()) {
                return emptyList()
            }

            val xList = values["x"]!!
            val yList = values["y"]!!
            val zList = values["z"]!!
            val minSize = minOf(xList.size, yList.size, zList.size)

            (0 until minSize).map { i ->
                Triple(xList[i], yList[i], zList[i])
            }
        } catch (e: Exception) {
            Log.w(TAG, "传感器数据解析失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 提取加速度计特征 (7个)
     */
    private fun extractAccelerometerFeatures(data: List<Triple<Float, Float, Float>>): Septuple<Float, Float, Float, Float, Float, Float, Float> {
        if (data.isEmpty()) {
            return Septuple(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }

        val xValues = data.map { it.first }
        val yValues = data.map { it.second }
        val zValues = data.map { it.third }

        val meanX = xValues.average().toFloat()
        val meanY = yValues.average().toFloat()
        val meanZ = zValues.average().toFloat()

        val stdX = calculateStd(xValues).toFloat()
        val stdY = calculateStd(yValues).toFloat()
        val stdZ = calculateStd(zValues).toFloat()

        // 加速度量级 = sqrt(x^2 + y^2 + z^2) 的平均值
        val magnitude = data.map { sqrt(it.first * it.first + it.second * it.second + it.third * it.third) }
            .average().toFloat()

        return Septuple(meanX, meanY, meanZ, stdX, stdY, stdZ, magnitude)
    }

    /**
     * 提取陀螺仪特征 (6个)
     */
    private fun extractGyroscopeFeatures(data: List<Triple<Float, Float, Float>>): Sextuple<Float, Float, Float, Float, Float, Float> {
        if (data.isEmpty()) {
            return Sextuple(0f, 0f, 0f, 0f, 0f, 0f)
        }

        val xValues = data.map { it.first }
        val yValues = data.map { it.second }
        val zValues = data.map { it.third }

        val meanX = xValues.average().toFloat()
        val meanY = yValues.average().toFloat()
        val meanZ = zValues.average().toFloat()

        val stdX = calculateStd(xValues).toFloat()
        val stdY = calculateStd(yValues).toFloat()
        val stdZ = calculateStd(zValues).toFloat()

        return Sextuple(meanX, meanY, meanZ, stdX, stdY, stdZ)
    }

    /**
     * 计算标准差
     */
    private fun calculateStd(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return Math.sqrt(variance)
    }

    @JvmName("calculateStdFloat")
    private fun calculateStd(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return Math.sqrt(variance).toFloat()
    }
}

// 辅助数据类用于返回多个值
data class Septuple<A, B, C, D, E, F, G>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G)

data class Sextuple<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
