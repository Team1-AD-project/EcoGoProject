# 交通方式智能检测模块

## 📁 模块结构

```
ml/
├── SensorData.kt                          # 数据模型定义
├── SensorFeatureExtractor.kt              # 特征提取器
├── SensorDataCollector.kt                 # 传感器数据采集器
├── TransportModeDetector.kt               # 交通方式检测器
├── TransportModeDetectorIntegration.kt    # 集成示例
└── README.md                              # 本文档
```

---

## 🎯 功能说明

### 1. 数据采集
- **采集频率**: 50 Hz（每 20ms 一次）
- **窗口大小**: 5 秒（250 个样本）
- **滑动步长**: 2.5 秒（50% 重叠）

### 2. 采集的传感器数据
| 传感器 | 数据 | 用途 |
|--------|------|------|
| 加速度计 | x, y, z 三轴加速度 | 检测运动模式（步行晃动、车辆震动） |
| 陀螺仪 | x, y, z 角速度 | 检测旋转（转弯、骑行摆动） |
| GPS | 速度 | 移动速度判断 |
| 气压计 | 气压值 | 检测地铁（地下气压变化） |

### 3. 提取的特征
- **统计特征**: 均值、标准差、最大值、最小值、范围、中位数、SMA
- **组合特征**: 加速度幅值、陀螺仪幅值、速度统计
- **总计**: 53 个特征

### 4. 支持的交通方式
- 步行 (WALKING)
- 骑行 (CYCLING)
- 公交 (BUS)
- 地铁 (SUBWAY)
- 驾车 (DRIVING)

---

## 🚀 快速开始

### 1. 基本使用

```kotlin
// 在 Activity 中初始化
class MapActivity : AppCompatActivity() {

    private lateinit var modeDetector: TransportModeDetectorIntegration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化检测器
        modeDetector = TransportModeDetectorIntegration(this) { prediction ->
            // 处理检测结果
            when (prediction.mode) {
                TransportModeLabel.WALKING -> handleWalking()
                TransportModeLabel.CYCLING -> handleCycling()
                TransportModeLabel.DRIVING -> handleDriving()
                // ...
            }
        }

        lifecycle.addObserver(modeDetector)
    }

    private fun startNavigation() {
        // 开始检测
        modeDetector.start()
    }

    private fun stopNavigation() {
        // 停止检测
        modeDetector.stop()
    }

    private fun onLocationUpdate(location: Location) {
        // 更新位置（用于获取 GPS 速度）
        modeDetector.updateLocation(location)
    }
}
```

### 2. 处理模式切换

```kotlin
private fun handleModeChange(prediction: TransportModePrediction) {
    val currentMode = viewModel.selectedTransportMode.value
    val detectedMode = prediction.mode

    // 只在置信度高且明显不符时提示
    if (prediction.confidence > 0.7f && isModeMismatch(currentMode, detectedMode)) {
        showModeSwitchDialog(detectedMode)
    }
}

private fun isModeMismatch(
    userSelected: TransportMode?,
    detected: TransportModeLabel
): Boolean {
    return when {
        userSelected == TransportMode.WALKING && detected == TransportModeLabel.DRIVING -> true
        userSelected == TransportMode.DRIVING && detected == TransportModeLabel.WALKING -> true
        else -> false
    }
}
```

---

## ⚠️ 当前状态

### ✅ 已完成
- ✅ 传感器数据采集框架
- ✅ 特征提取算法（53 个特征）
- ✅ 数据窗口生成（5 秒窗口，2.5 秒滑动）
- ✅ 预测结果平滑（多数投票）
- ✅ 基于规则的临时分类器

### ⚠️ 待完成
- ⚠️ **Random Forest 模型训练**（需要标注数据）
- ⚠️ **TensorFlow Lite 模型集成**
- ⚠️ 数据标注工具（用户标记交通方式）
- ⚠️ 模型性能评估和优化

---

## 🔧 下一步：训练 Random Forest 模型

### 步骤 1: 数据收集

在 APP 中添加"数据标注模式"：

```kotlin
class DataCollectionActivity : AppCompatActivity() {

    private val collector = SensorDataCollector(this)
    private var currentLabel = TransportModeLabel.WALKING

    fun startRecording(label: TransportModeLabel) {
        currentLabel = label
        collector.startCollecting()

        // 每个窗口自动保存
        lifecycleScope.launch {
            collector.windowFlow.collect { window ->
                window?.let {
                    collector.saveWindowForTraining(it, currentLabel)
                }
            }
        }
    }
}
```

**数据收集建议**：
- 每种交通方式: 至少 2-3 小时的数据
- 不同用户: 10-20 人
- 不同场景: 早晚高峰、平峰、周末

### 步骤 2: 数据导出

将收集的数据导出为 CSV 格式：

```csv
accXMean,accXStd,accXMax,...,gpsSpeedMean,pressureStd,label
0.15,1.23,2.45,...,1.5,0.03,WALKING
0.08,0.85,1.89,...,6.8,0.02,CYCLING
...
```

### 步骤 3: 模型训练（Python）

使用 scikit-learn 训练 Random Forest：

```python
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report

# 1. 加载数据
data = pd.read_csv('sensor_data.csv')
X = data.drop('label', axis=1)
y = data['label']

# 2. 分割训练集和测试集
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# 3. 训练 Random Forest
model = RandomForestClassifier(
    n_estimators=100,      # 100 棵树
    max_depth=20,          # 最大深度
    min_samples_split=5,
    random_state=42
)
model.fit(X_train, y_train)

# 4. 评估
y_pred = model.predict(X_test)
accuracy = accuracy_score(y_test, y_pred)
print(f"Accuracy: {accuracy:.2f}")
print(classification_report(y_test, y_pred))

# 5. 导出模型
import joblib
joblib.dump(model, 'transport_mode_classifier.pkl')
```

### 步骤 4: 转换为 TensorFlow Lite

```python
import tensorflow as tf
from sklearn.ensemble import RandomForestClassifier
import joblib

# 加载 sklearn 模型
rf_model = joblib.load('transport_mode_classifier.pkl')

# 转换为 TensorFlow 模型（需要使用 tf-decision-forests）
import tensorflow_decision_forests as tfdf

# 或者直接在 Android 中使用 sklearn-porter
# 将 Random Forest 转换为 Java/Kotlin 代码
```

### 步骤 5: 集成到 Android

```kotlin
class TransportModeDetector(context: Context) {

    // 加载 TFLite 模型
    private val interpreter: Interpreter by lazy {
        val model = loadModelFile(context, "transport_classifier.tflite")
        Interpreter(model)
    }

    private fun predictTransportMode(features: SensorFeatures): TransportModePrediction {
        // 准备输入
        val input = features.toFloatArray()
        val inputBuffer = FloatBuffer.wrap(input)

        // 准备输出
        val output = FloatArray(5)  // 5 个类别
        val outputBuffer = FloatBuffer.wrap(output)

        // 运行推理
        interpreter.run(inputBuffer, outputBuffer)

        // 解析结果
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: 0
        val mode = TransportModeLabel.values()[maxIndex]
        val confidence = output[maxIndex]

        return TransportModePrediction(mode, confidence, ...)
    }
}
```

---

## 📊 性能优化

### 电量优化
```kotlin
// 降低采样频率
private val samplingIntervalMs = 100L  // 从 50ms 改为 100ms (10 Hz)

// 或者只在导航时启用
if (isNavigating) {
    detector.startDetection()
} else {
    detector.stopDetection()
}
```

### 内存优化
```kotlin
// 限制缓冲区大小
private val maxBufferSize = 300  // 最多保留 300 个样本
```

---

## 🐛 调试

### 查看传感器数据
```kotlin
lifecycleScope.launch {
    detector.detectedMode.collect { prediction ->
        Log.d("ML", "Mode: ${prediction?.mode}, Confidence: ${prediction?.confidence}")
    }
}
```

### 导出特征用于分析
```kotlin
val features = SensorFeatureExtractor.extractFeatures(window)
val json = Gson().toJson(features)
Log.d("Features", json)
```

---

## 📚 参考资料

- [Human Activity Recognition](https://www.tensorflow.org/lite/examples/activity_recognition/overview)
- [Transportation Mode Detection Paper](https://arxiv.org/abs/1804.05069)
- [Random Forest Documentation](https://scikit-learn.org/stable/modules/ensemble.html#forest)
- [TensorFlow Lite](https://www.tensorflow.org/lite/android)

---

## 💡 常见问题

**Q: 为什么使用 Random Forest 而不是深度学习？**
A: Random Forest 更适合移动端：模型更小、推理更快、不需要 GPU、更容易解释。

**Q: 需要多少训练数据？**
A: 建议每个类别至少 2-3 小时，总计 10-15 小时的标注数据。

**Q: 准确率能达到多少？**
A: 使用 Random Forest，预期准确率 85-92%。如果使用深度学习，可以达到 90-95%。

**Q: 模型多久更新一次预测？**
A: 每 2.5 秒生成一个新窗口，但使用 3 次预测的多数投票，所以实际上每 7.5 秒输出一个稳定的结果。

---

## 📝 TODO

- [ ] 实现数据标注界面
- [ ] 收集训练数据（10+ 小时）
- [ ] 训练 Random Forest 模型
- [ ] 转换为 TensorFlow Lite
- [ ] 集成模型到 APP
- [ ] 性能测试和优化
- [ ] 用户测试和反馈收集
