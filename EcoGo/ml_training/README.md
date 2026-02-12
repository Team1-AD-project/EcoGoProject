# 交通方式识别模型训练指南

这个目录包含了训练 Random Forest 模型的完整流程。

---

## 📋 完整流程

```
步骤 1: 收集数据 (Android APP)
    ↓
步骤 2: 导出 CSV 文件
    ↓
步骤 3: 训练模型 (Python)
    ↓
步骤 4: 集成到 Android
```

---

## 1️⃣ 收集训练数据

### 在 Android APP 中打开数据收集界面

```kotlin
// 在 MapActivity 的菜单或按钮中添加
val intent = Intent(this, DataCollectionActivity::class.java)
startActivity(intent)
```

### 数据收集步骤

1. **选择交通方式**：步行、骑行、公交、地铁、驾车
2. **点击"开始记录"**
3. **进行该交通方式的出行**（建议 10-20 分钟）
4. **点击"停止记录"**
5. **重复步骤 1-4**，收集所有交通方式的数据

### 数据收集建议

| 交通方式 | 建议时长 | 场景 |
|---------|---------|------|
| 步行 | 20 分钟 | 正常步行、快走 |
| 骑行 | 15 分钟 | 自行车、电动车 |
| 公交 | 20 分钟 | 至少 2-3 个站点 |
| 地铁 | 20 分钟 | 至少 2-3 个站点 |
| 驾车 | 20 分钟 | 市区道路、高速公路 |

**总计**: 至少 1.5-2 小时的数据

---

## 2️⃣ 导出数据

### 在 DataCollectionActivity 中

1. 收集完所有数据后，点击 **"导出训练数据"**
2. APP 会生成 CSV 文件，路径类似：
   ```
   /sdcard/Android/data/com.ecogo.app/files/ml_training_data/sensor_data_xxxxx.csv
   ```

### 传输数据到电脑

使用 adb 命令：

```bash
# 1. 找到数据文件
adb shell ls /sdcard/Android/data/com.ecogo.app/files/ml_training_data/

# 2. 拉取到电脑
adb pull /sdcard/Android/data/com.ecogo.app/files/ml_training_data/sensor_data_xxxxx.csv ./data/

# 或者一次性拉取所有
adb pull /sdcard/Android/data/com.ecogo.app/files/ml_training_data/ ./data/
```

或者使用文件管理器手动复制。

---

## 3️⃣ 训练模型

### 环境准备

```bash
# 创建虚拟环境
python3 -m venv venv
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 安装依赖
pip install -r requirements.txt
```

### 准备数据

将所有 CSV 文件放入 `data/` 目录：

```
ml_training/
├── data/
│   ├── sensor_data_1234567890.csv
│   ├── sensor_data_2345678901.csv
│   └── sensor_data_3456789012.csv
├── train_random_forest.py
└── requirements.txt
```

### 开始训练

```bash
python train_random_forest.py
```

### 训练输出

脚本会输出：

```
==============================================================
交通方式分类器训练
==============================================================

找到 3 个数据文件
  - sensor_data_1234567890.csv: 240 条记录
  - sensor_data_2345678901.csv: 180 条记录
  - sensor_data_3456789012.csv: 200 条记录

总共加载 620 条记录

==============================================================
数据分析
==============================================================

类别分布:
  WALKING   : 150 (24.2%)
  CYCLING   : 120 (19.4%)
  BUS       : 130 (21.0%)
  SUBWAY    :  90 (14.5%)
  DRIVING   : 130 (21.0%)

✅ 没有缺失值
✅ 数据相对平衡 (比例: 1.7:1)

训练集大小: 496 条记录
测试集大小: 124 条记录

==============================================================
训练 Random Forest 模型
==============================================================
参数:
  - 树的数量: 100
  - 最大深度: 20
  - 最小分裂样本数: 5

[Parallel(n_jobs=-1)]: Done 100 out of 100 | elapsed:    0.5s finished

==============================================================
模型评估
==============================================================

训练集准确率: 0.9879 (98.79%)
测试集准确率: 0.8871 (88.71%)

5折交叉验证准确率: 0.8790 (±0.0234)
F1 分数 (weighted): 0.8856

详细分类报告:
==============================================================
              precision    recall  f1-score   support

     WALKING       0.93      0.90      0.91        30
     CYCLING       0.85      0.88      0.86        24
         BUS       0.88      0.84      0.86        25
      SUBWAY       0.83      0.83      0.83        18
     DRIVING       0.89      0.93      0.91        27

    accuracy                           0.89       124
   macro avg       0.88      0.88      0.88       124
weighted avg       0.89      0.89      0.89       124

✅ 混淆矩阵已保存: confusion_matrix.png
✅ 特征重要性已保存: feature_importance.png

前 10 个最重要的特征:
  1. gpsSpeedMean         : 0.1523
  2. accMagnitudeMean     : 0.0856
  3. gpsSpeedStd          : 0.0743
  4. accXStd              : 0.0621
  5. accYStd              : 0.0598
  6. gyroMagnitudeStd     : 0.0512
  7. accZMean             : 0.0487
  8. gpsSpeedMax          : 0.0456
  9. accMagnitudeStd      : 0.0423
  10. gyroXStd            : 0.0398

✅ 模型已保存: transport_mode_classifier.pkl
✅ 标签编码器已保存: label_encoder.pkl
   模型文件大小: 1234.5 KB

==============================================================
训练完成！
==============================================================
✅ 测试集准确率: 88.71%
✅ 模型文件: transport_mode_classifier.pkl
✅ 标签编码器: label_encoder.pkl
```

---

## 4️⃣ 集成模型到 Android

### 方案 A：使用 sklearn-porter（推荐）

由于 TensorFlow Lite 不直接支持 sklearn 的 Random Forest，我们使用 **sklearn-porter** 将模型转换为 Java 代码。

#### 安装 sklearn-porter

```bash
pip install sklearn-porter
```

#### 转换模型

```python
from sklearn_porter import Porter
import joblib

# 加载训练好的模型
model = joblib.load('transport_mode_classifier.pkl')

# 转换为 Java 代码
porter = Porter(model, language='java')
java_code = porter.export()

# 保存为 Java 文件
with open('RandomForestClassifier.java', 'w') as f:
    f.write(java_code)
```

#### 在 Android 中使用

1. 将生成的 `RandomForestClassifier.java` 复制到 Android 项目
2. 修改 `TransportModeDetector.kt` 中的 `predictTransportMode()` 方法

```kotlin
private fun predictTransportMode(features: SensorFeatures): TransportModePrediction {
    val featureArray = features.toFloatArray()

    // 使用生成的 Java 代码进行预测
    val prediction = RandomForestClassifier.predict(featureArray.toDoubleArray())

    // 转换预测结果
    val mode = when (prediction) {
        0 -> TransportModeLabel.WALKING
        1 -> TransportModeLabel.CYCLING
        2 -> TransportModeLabel.BUS
        3 -> TransportModeLabel.SUBWAY
        4 -> TransportModeLabel.DRIVING
        else -> TransportModeLabel.UNKNOWN
    }

    // 获取概率（如果模型支持）
    val probabilities = RandomForestClassifier.predictProba(featureArray.toDoubleArray())
    val confidence = probabilities.maxOrNull()?.toFloat() ?: 0.5f

    return TransportModePrediction(
        mode = mode,
        confidence = confidence,
        probabilities = mapOf(/* ... */)
    )
}
```

---

### 方案 B：使用简化的决策树

如果 sklearn-porter 不work，可以手动实现一个简化的决策树分类器：

```kotlin
object TransportModeClassifier {

    fun predict(features: FloatArray): Int {
        // 这是从训练好的模型中提取的决策规则
        // 可以从 sklearn 的决策树中导出

        val gpsSpeedMean = features[45]  // 索引 45 是 gpsSpeedMean
        val accMagnitudeMean = features[42]  // 索引 42 是 accMagnitudeMean
        val gpsSpeedStd = features[46]  // 索引 46 是 gpsSpeedStd

        return when {
            gpsSpeedMean < 2.0f -> {  // < 7.2 km/h
                if (accMagnitudeMean > 1.2f) 0  // WALKING
                else 5  // UNKNOWN
            }
            gpsSpeedMean < 7.0f -> {  // < 25.2 km/h
                if (accMagnitudeMean > 0.5f) 1  // CYCLING
                else if (gpsSpeedStd > 3.0f) 2  // BUS
                else 1  // CYCLING
            }
            gpsSpeedMean < 17.0f -> {  // < 61.2 km/h
                if (gpsSpeedStd > 5.0f) 2  // BUS
                else 3  // SUBWAY
            }
            else -> 4  // DRIVING
        }
    }
}
```

---

## 📊 评估模型质量

### 好的模型指标

- ✅ **测试集准确率** ≥ 85%
- ✅ **交叉验证准确率** ≥ 80%
- ✅ **F1 分数** ≥ 0.80
- ✅ 各类别的 precision 和 recall 都 ≥ 0.75

### 如果准确率不够

1. **收集更多数据**：每个类别至少 30 分钟
2. **平衡数据**：确保每个类别的数据量相近
3. **增加特征**：添加频域特征
4. **调整参数**：增加树的数量（`N_ESTIMATORS = 200`）

---

## 🐛 常见问题

**Q: 训练准确率很高（99%）但测试准确率低（70%）**
A: 这是过拟合。减小 `MAX_DEPTH`（如改为 15）或增加 `MIN_SAMPLES_SPLIT`（如改为 10）。

**Q: 某些类别的 recall 很低**
A: 该类别的数据太少。多收集该类别的数据。

**Q: 如何在 Android 中使用 .pkl 模型**
A: sklearn 的 .pkl 文件不能直接在 Android 中使用。需要转换为 Java 代码（方案 A）或提取决策规则（方案 B）。

---

## 📚 参考资料

- [scikit-learn Random Forest](https://scikit-learn.org/stable/modules/generated/sklearn.ensemble.RandomForestClassifier.html)
- [sklearn-porter](https://github.com/nok/sklearn-porter)
- [人类活动识别论文](https://arxiv.org/abs/1804.05069)

---

## ✅ 检查清单

训练前:
- [ ] 收集了至少 5 种交通方式的数据
- [ ] 每种方式至少 15 分钟数据
- [ ] CSV 文件已放入 data/ 目录

训练后:
- [ ] 测试集准确率 ≥ 85%
- [ ] 各类别 F1 分数 ≥ 0.75
- [ ] 生成了混淆矩阵图
- [ ] 生成了特征重要性图

集成前:
- [ ] 模型已转换为 Java 代码或决策规则
- [ ] 在 Android 中测试了预测功能
- [ ] 预测结果符合预期
