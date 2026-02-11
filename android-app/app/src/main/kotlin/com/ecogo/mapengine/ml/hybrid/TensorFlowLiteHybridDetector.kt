package com.ecogo.mapengine.ml.hybrid

import android.content.Context
import android.util.Log
import com.ecogo.mapengine.ml.HybridTransportModeDetector
import com.ecogo.mapengine.ml.model.JourneyFeatures
import com.ecogo.mapengine.ml.tflite.TensorFlowLiteDetector
import com.ecogo.mapengine.ml.tflite.TransportModePrediction
import com.ecogo.mapengine.ml.training.FeatureExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 增强型混合检测器 - 集成TensorFlow Lite模型
 *
 * 工作流程：
 * 1. 收集GPS点和传感器数据
 * 2. 提取特征向量（14维）
 * 3. 调用TensorFlow Lite模型进行推理
 * 4. 返回5个交通方式的概率分布
 *
 * 置信度处理：
 * - TFLite置信度 ≥ 0.85 → 直接使用
 * - TFLite置信度 0.65-0.85 → 与Snap to Roads融合
 * - TFLite置信度 < 0.65 → 回退到Snap to Roads
 *
 * 注意：需要先将model.tflite放在assets文件夹中
 */
class TensorFlowLiteHybridDetector(
    private val context: Context,
    private val featureExtractor: FeatureExtractor
) {
    
    companion object {
        private const val TAG = "TFLiteHybridDetector"
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.65f
    }
    
    private var tfliteDetector: TensorFlowLiteDetector? = null
    private var isInitialized = false
    
    /**
     * 初始化TensorFlow Lite检测器
     * 需要在使用前调用
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        if (isInitialized) {
            return@withContext true
        }
        
        return@withContext try {
            tfliteDetector = TensorFlowLiteDetector(context)
            val success = tfliteDetector?.loadModel() ?: false
            
            if (success) {
                isInitialized = true
                Log.d(TAG, "TensorFlow Lite检测器初始化成功")
            } else {
                Log.w(TAG, "TensorFlow Lite模型加载失败，将回退到Snap to Roads")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 使用TensorFlow Lite进行推理
     *
     * @param features 提取的特征向量
     * @return 预测结果，包含置信度和交通方式概率
     */
    suspend fun predictWithTFLite(features: JourneyFeatures): TransportModePrediction? {
        if (!isInitialized || tfliteDetector == null) {
            Log.w(TAG, "TFLite检测器未初始化")
            return null
        }
        
        return withContext(Dispatchers.Default) {
            try {
                val prediction = tfliteDetector?.predict(features)
                if (prediction != null) {
                    Log.d(TAG, "TFLite预测: ${prediction.predictedMode} (${prediction.confidence})")
                }
                prediction
            } catch (e: Exception) {
                Log.e(TAG, "推理失败: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * 融合TFLite预测和Snap to Roads结果
     *
     * 融合策略：
     * 1. 如果TFLite置信度高，直接使用TFLite结果
     * 2. 如果TFLite和Snap to Roads都指向同一交通方式，增加置信度
     * 3. 如果两者不一致，结合其他信号（速度、道路类型）进行仲裁
     *
     * @param tflitePrediction TFLite预测结果
     * @param snapPrediction Snap to Roads预测结果 (交通方式字符串)
     * @param avgSpeed 平均速度 (m/s)
     * @return 最终预测结果
     */
    suspend fun fusePredictions(
        tflitePrediction: TransportModePrediction?,
        snapPrediction: String,
        avgSpeed: Float
    ): Pair<String, Float> = withContext(Dispatchers.Default) {
        
        // 如果TFLite预测不可用，直接使用Snap to Roads
        if (tflitePrediction == null) {
            Log.d(TAG, "TFLite预测不可用，使用Snap to Roads: $snapPrediction")
            return@withContext Pair(snapPrediction, 0.8f)
        }
        
        // 策略1：TFLite置信度很高，直接使用
        if (tflitePrediction.confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "TFLite置信度高，直接使用: ${tflitePrediction.predictedMode}")
            return@withContext Pair(tflitePrediction.predictedMode, tflitePrediction.confidence)
        }
        
        // 策略2：TFLite和Snap to Roads一致
        if (tflitePrediction.predictedMode == snapPrediction) {
            // 两者一致，提升置信度
            val boostedConfidence = (tflitePrediction.confidence + 0.8f) / 2
            Log.d(TAG, "两个模型一致，提升置信度到 ${String.format("%.2f", boostedConfidence)}")
            return@withContext Pair(tflitePrediction.predictedMode, boostedConfidence)
        }
        
        // 策略3：TFLite置信度中等，考虑融合
        if (tflitePrediction.confidence >= LOW_CONFIDENCE_THRESHOLD) {
            // 根据速度信息和道路类型进行仲裁
            val finalMode = arbitrateConflict(
                tflitePrediction.predictedMode,
                snapPrediction,
                avgSpeed
            )
            
            Log.d(TAG, "低置信度融合: TFLite=${tflitePrediction.predictedMode}, " +
                    "Snap=$snapPrediction -> $finalMode")
            
            return@withContext Pair(finalMode, tflitePrediction.confidence)
        }
        
        // 策略4：TFLite置信度低，使用Snap to Roads为主
        Log.d(TAG, "TFLite置信度低 (${tflitePrediction.confidence}), 使用Snap to Roads")
        return@withContext Pair(snapPrediction, 0.75f)
    }
    
    /**
     * 仲裁冲突的预测
     * 当两个模型给出不同的结果时，结合速度等信息做出决策
     */
    private fun arbitrateConflict(tfliteMode: String, snapMode: String, avgSpeed: Float): String {
        // 规则1：地铁很难从速度判断，优先信任TFLite
        if (tfliteMode == "SUBWAY" && avgSpeed in 3.0..15.0) {
            return tfliteMode
        }
        
        // 规则2：高速行驶，相信Snap to Roads的道路类型判断
        if (avgSpeed > 20) {
            return snapMode
        }
        
        // 规则3：TFLite模型已在足够多的数据上训练，优先信任
        return tfliteMode
    }
    
    /**
     * 获取模型状态信息
     */
    fun getModelStatus(): String {
        return """
            TensorFlow Lite混合检测器状态：
            - 初始化: ${if (isInitialized) "✓ 已初始化" else "✗ 未初始化"}
            - 模型加载: ${if (tfliteDetector != null) "✓ 已加载" else "✗ 未加载"}
            - 可用状态: ${if (isInitialized && tfliteDetector != null) "✓ 可用" else "✗ 不可用"}
        """.trimIndent()
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            tfliteDetector?.release()
            isInitialized = false
            Log.d(TAG, "TFLite检测器资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败: ${e.message}", e)
        }
    }
}

/**
 * 扩展函数：为HybridTransportModeDetector添加TFLite支持
 * 使用方式：
 * hybridDetector.enableTensorFlowLite(context)
 */
suspend fun HybridTransportModeDetector.enableTensorFlowLite(context: Context): Boolean {
    // 这个扩展函数需要修改HybridTransportModeDetector来支持
    // 或者在HybridTransportModeDetector内部创建TensorFlowLiteHybridDetector实例
    return false
}
