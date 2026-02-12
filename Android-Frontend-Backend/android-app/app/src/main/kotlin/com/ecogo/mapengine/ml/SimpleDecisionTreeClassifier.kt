package com.ecogo.mapengine.ml


object SimpleDecisionTreeClassifier {

    /**
     * 预测交通方式
     *
     * @param features 53 维特征数组
     * @return 预测的类别索引 (0=WALKING, 1=CYCLING, 2=BUS, 3=SUBWAY, 4=DRIVING)
     */
    fun predict(features: FloatArray): Pair<Int, Float> {
        // 提取关键特征
        // 特征索引参考 SensorFeatures.toFloatArray():
        // [42-44] accMagnitude(Mean/Std/Max), [45-47] gyroMagnitude(Mean/Std/Max),
        // [48-50] gpsSpeed(Mean/Std/Max), [51-52] pressure(Mean/Std)
        val accMagnitudeMean = features[42]
        val accMagnitudeStd = features[43]
        val accMagnitudeMax = features[44]

        val gyroMagnitudeMean = features[45]
        val gyroMagnitudeStd = features[46]

        val gpsSpeedMean = features[48] * 3.6f  // m/s -> km/h
        val gpsSpeedStd = features[49] * 3.6f
        val gpsSpeedMax = features[50] * 3.6f

        val pressureStd = features[52]

        // ====================================================================
        // 这里是临时的规则分类器
        // 训练完 Random Forest 后，替换为模型的决策逻辑
        // ====================================================================

        return when {
            // 步行：低速 + 明显且有规律的震动
            gpsSpeedMean < 7f && accMagnitudeMean > 1.0f && accMagnitudeStd > 0.4f -> {
                0 to calculateConfidence(gpsSpeedMean, 0f, 7f, accMagnitudeMean, 1.0f, 3.0f)
            }

            // 骑行：中低速 + 中等以上震动（骑车身体颠簸比坐车大）
            // 去掉 gpsSpeedStd < 4 的限制：骑车遇红灯也会频繁停启导致速度波动
            // 关键区分点：骑车 accMagnitudeMean > 0.3（身体颠簸），公交 < 0.5（车内平稳）
            gpsSpeedMean in 7f..25f && accMagnitudeMean > 0.3f -> {
                1 to calculateConfidence(gpsSpeedMean, 7f, 25f, accMagnitudeMean, 0.3f, 1.2f)
            }

            // 公交：中高速 + 频繁停启动 + 低震动（乘客坐在车内，身体平稳）
            // 提高速度下限到 20 km/h，降低 accMagnitudeMean 阈值到 0.5
            // 避免把骑行误判为公交
            gpsSpeedMean in 20f..60f && gpsSpeedStd > 4f && accMagnitudeMean < 0.5f -> {
                2 to calculateConfidence(gpsSpeedStd, 4f, 15f, gpsSpeedMean, 20f, 60f)
            }

            // 地铁：高速 + 气压变化明显 + 平稳
            gpsSpeedMean > 25f && pressureStd > 3f && accMagnitudeMean < 0.6f -> {
                3 to calculateConfidence(gpsSpeedMean, 25f, 80f, pressureStd, 3f, 15f)
            }

            // 驾车：中高速 + 平稳 + 速度相对稳定
            gpsSpeedMean > 25f && accMagnitudeMean < 0.7f && gpsSpeedStd < 10f -> {
                4 to calculateConfidence(gpsSpeedMean, 25f, 100f, accMagnitudeMean, 0f, 0.7f)
            }

            // 默认：根据速度判断最可能的类别
            else -> {
                when {
                    gpsSpeedMean < 7f -> 0 to 0.5f  // 可能是步行
                    gpsSpeedMean < 25f -> 1 to 0.5f  // 可能是骑行
                    gpsSpeedMean < 60f -> 2 to 0.5f  // 可能是公交
                    else -> 4 to 0.5f  // 可能是驾车
                }
            }
        }
    }

    /**
     * 计算置信度
     * 基于特征值在预期范围内的程度
     */
    private fun calculateConfidence(
        value1: Float, min1: Float, max1: Float,
        value2: Float, min2: Float, max2: Float
    ): Float {
        // 归一化到 [0, 1]
        val score1 = ((value1 - min1) / (max1 - min1)).coerceIn(0f, 1f)
        val score2 = ((value2 - min2) / (max2 - min2)).coerceIn(0f, 1f)

        // 平均分数，映射到 [0.6, 0.95] 范围
        val avgScore = (score1 + score2) / 2f
        return 0.6f + avgScore * 0.35f
    }

    /**
     * 预测概率分布（简化版）
     *
     * TODO: 训练完 Random Forest 后，使用 predict_proba() 的结果
     */
    fun predictProba(features: FloatArray): FloatArray {
        val (predictedClass, confidence) = predict(features)

        // 创建概率数组
        val probabilities = FloatArray(5) { 0f }

        // 将置信度分配给预测的类别
        probabilities[predictedClass] = confidence

        // 剩余概率平均分配给其他类别
        val remainingProb = 1f - confidence
        val probPerOther = remainingProb / 4

        for (i in probabilities.indices) {
            if (i != predictedClass) {
                probabilities[i] = probPerOther
            }
        }

        return probabilities
    }
}

/**
 * 使用示例：
 *
 * ```kotlin
 * val features = SensorFeatureExtractor.extractFeatures(window)
 * val featureArray = features.toFloatArray()
 *
 * val (predictedClass, confidence) = SimpleDecisionTreeClassifier.predict(featureArray)
 *
 * val mode = when (predictedClass) {
 *     0 -> TransportModeLabel.WALKING
 *     1 -> TransportModeLabel.CYCLING
 *     2 -> TransportModeLabel.BUS
 *     3 -> TransportModeLabel.SUBWAY
 *     4 -> TransportModeLabel.DRIVING
 *     else -> TransportModeLabel.UNKNOWN
 * }
 *
 * println("预测: $mode, 置信度: $confidence")
 * ```
 */
