package com.ecogo.mapengine.ml.tflite

import android.content.Context
import android.util.Log
import com.ecogo.mapengine.ml.model.JourneyFeatures
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TensorFlow Lite模型推理引擎
 *
 * 工作流程：
 * 1. 从assets文件夹加载model.tflite
 * 2. 初始化Interpreter
 * 3. 接收特征向量（17维）
 * 4. 输出5个交通方式的概率分布
 *
 * 注意：当前为框架代码，实际模型文件需要通过Python训练脚本生成
 */
class TensorFlowLiteDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "TFLiteDetector"
        private const val MODEL_FILE = "transport_mode_model.tflite"
        
        // 输入输出维度
        private const val INPUT_FEATURES = 17      // 输入特征维度 (14传感器 + 3 GPS速度)
        private const val OUTPUT_CLASSES = 5       // 5个交通方式类别
        
        // 交通方式索引
        private val TRANSPORT_MODES = listOf(
            "WALKING",
            "CYCLING",
            "BUS",
            "DRIVING",
            "SUBWAY"
        )
    }
    
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    
    /**
     * 初始化模型，加载TensorFlow Lite文件
     */
    fun loadModel(): Boolean {
        return try {
            if (interpreter != null) {
                Log.d(TAG, "模型已加载")
                return true
            }
            
            // 从assets加载模型文件
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            interpreter = Interpreter(modelBuffer)
            isModelLoaded = true
            
            Log.d(TAG, "TensorFlow Lite模型加载成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "模型加载失败: ${e.message}", e)
            isModelLoaded = false
            false
        }
    }
    
    /**
     * 预测交通方式
     *
     * @param features 特征向量 (17维)
     * @return TransportModePrediction包含5个交通方式的概率 + 最可能的模式
     */
    fun predict(features: JourneyFeatures): TransportModePrediction? {
        if (!isModelLoaded || interpreter == null) {
            Log.w(TAG, "模型未加载，无法预测")
            return null
        }
        
        return try {
            // 步骤1：构建输入特征向量 (17维)
            val inputFeatures = floatArrayOf(
                // 加速度计特征 (7个)
                features.accelMeanX,
                features.accelMeanY,
                features.accelMeanZ,
                features.accelStdX,
                features.accelStdY,
                features.accelStdZ,
                features.accelMagnitude,

                // 陀螺仪特征 (6个)
                features.gyroMeanX,
                features.gyroMeanY,
                features.gyroMeanZ,
                features.gyroStdX,
                features.gyroStdY,
                features.gyroStdZ,

                // 时间特征 (1个)
                features.journeyDuration,

                // GPS速度特征 (3个)
                features.gpsSpeedMean,
                features.gpsSpeedStd,
                features.gpsSpeedMax
            )
            
            // 步骤2：验证特征维度
            if (inputFeatures.size != INPUT_FEATURES) {
                Log.e(TAG, "特征维度不匹配: ${inputFeatures.size} != $INPUT_FEATURES")
                return null
            }
            
            // 步骤3：创建输入缓冲区
            val inputBuffer = ByteBuffer.allocateDirect(INPUT_FEATURES * 4) // 4字节per float
                .order(ByteOrder.nativeOrder())
            inputBuffer.asFloatBuffer().put(inputFeatures)
            
            // 步骤4：创建输出缓冲区 (5个类别的概率)
            val outputBuffer = Array(1) { FloatArray(OUTPUT_CLASSES) }
            
            // 步骤5：运行推理
            interpreter!!.run(inputBuffer, outputBuffer)
            
            // 步骤6：处理输出概率
            val probabilities = outputBuffer[0]
            val predictions = TRANSPORT_MODES.indices.associateWith { probabilities[it] }
            
            // 步骤7：找出最可能的交通方式
            val maxEntry = predictions.maxByOrNull { it.value }
            val maxIndex = maxEntry?.key ?: 0
            val maxProb = maxEntry?.value ?: 0f
            val predictedMode = TRANSPORT_MODES[maxIndex]
            
            Log.d(TAG, "预测结果: $predictedMode (概率: $maxProb)")
            
            // 步骤8：构建返回结果
            TransportModePrediction(
                predictedMode = predictedMode,
                confidence = maxProb,
                probabilities = mapOf(
                    "WALKING" to probabilities[0],
                    "CYCLING" to probabilities[1],
                    "BUS" to probabilities[2],
                    "DRIVING" to probabilities[3],
                    "SUBWAY" to probabilities[4]
                ),
                inputFeatures = inputFeatures
            )
        } catch (e: Exception) {
            Log.e(TAG, "推理失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 批量预测（用于验证模型性能）
     */
    suspend fun batchPredict(featuresList: List<JourneyFeatures>): List<TransportModePrediction> {
        return featuresList.mapNotNull { predict(it) }
    }
    
    /**
     * 获取模型信息
     */
    fun getModelInfo(): String {
        return """
            TensorFlow Lite 模型信息：
            - 输入维度: ${INPUT_FEATURES}个特征
            - 输出类别: $OUTPUT_CLASSES (WALKING, CYCLING, BUS, DRIVING, SUBWAY)
            - 模型文件: $MODEL_FILE
            - 加载状态: ${if (isModelLoaded) "已加载" else "未加载"}
        """.trimIndent()
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            interpreter?.close()
            interpreter = null
            isModelLoaded = false
            Log.d(TAG, "模型资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败: ${e.message}", e)
        }
    }
}

/**
 * 预测结果数据类
 */
data class TransportModePrediction(
    val predictedMode: String,                    // 预测的交通方式
    val confidence: Float,                        // 置信度 (0-1)
    val probabilities: Map<String, Float>,       // 各交通方式的概率分布
    val inputFeatures: FloatArray? = null        // 输入特征（用于调试）
) {
    override fun toString(): String {
        val probStr = probabilities.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key}: ${String.format("%.1f%%", it.value * 100)}" }
        
        return """
            预测结果：
            - 模式: $predictedMode
            - 置信度: ${String.format("%.1f%%", confidence * 100)}
            - 概率分布: $probStr
        """.trimIndent()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as TransportModePrediction
        
        if (predictedMode != other.predictedMode) return false
        if (confidence != other.confidence) return false
        if (probabilities != other.probabilities) return false
        if (inputFeatures != null) {
            if (other.inputFeatures == null) return false
            if (!inputFeatures.contentEquals(other.inputFeatures)) return false
        } else if (other.inputFeatures != null) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = predictedMode.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + probabilities.hashCode()
        result = 31 * result + (inputFeatures?.contentHashCode() ?: 0)
        return result
    }
}
