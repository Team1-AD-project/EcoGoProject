#!/usr/bin/env python3
"""
交通方式分类器训练脚本
使用 Random Forest 模型训练交通方式识别器

使用方法:
1. 从 Android APP 导出 CSV 数据文件
2. 将所有 CSV 文件放在 data/ 目录下
3. 运行此脚本: python train_random_forest.py
4. 训练完成后，模型会保存为 transport_mode_classifier.pkl
"""

import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score
)
from sklearn.preprocessing import LabelEncoder
import joblib
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
import glob

# 配置
DATA_DIR = "data"  # CSV 数据文件目录
MODEL_OUTPUT = "transport_mode_classifier.pkl"
LABEL_ENCODER_OUTPUT = "label_encoder.pkl"

# Random Forest 参数
N_ESTIMATORS = 100      # 树的数量
MAX_DEPTH = 20          # 最大深度
MIN_SAMPLES_SPLIT = 5   # 最小分裂样本数
RANDOM_STATE = 42


def load_data(data_dir):
    """
    加载所有 CSV 数据文件并合并
    """
    csv_files = glob.glob(f"{data_dir}/*.csv")

    if not csv_files:
        raise FileNotFoundError(f"在 {data_dir} 目录下没有找到 CSV 文件")

    print(f"找到 {len(csv_files)} 个数据文件")

    # 读取并合并所有 CSV
    dfs = []
    for file in csv_files:
        df = pd.read_csv(file)
        dfs.append(df)
        print(f"  - {Path(file).name}: {len(df)} 条记录")

    data = pd.concat(dfs, ignore_index=True)
    print(f"\n总共加载 {len(data)} 条记录\n")

    return data


def analyze_data(data):
    """
    分析数据分布
    """
    print("=" * 60)
    print("数据分析")
    print("=" * 60)

    # 类别分布
    label_counts = data['label'].value_counts()
    print("\n类别分布:")
    for label, count in label_counts.items():
        percentage = count / len(data) * 100
        print(f"  {label:10s}: {count:5d} ({percentage:5.1f}%)")

    # 检查是否有缺失值
    missing = data.isnull().sum()
    if missing.any():
        print("\n⚠️  发现缺失值:")
        print(missing[missing > 0])
    else:
        print("\n✅ 没有缺失值")

    # 检查类别不平衡
    min_count = label_counts.min()
    max_count = label_counts.max()
    imbalance_ratio = max_count / min_count

    if imbalance_ratio > 3:
        print(f"\n⚠️  数据不平衡 (比例: {imbalance_ratio:.1f}:1)")
        print("   建议: 收集更多少数类别的数据")
    else:
        print(f"\n✅ 数据相对平衡 (比例: {imbalance_ratio:.1f}:1)")

    print()


def preprocess_data(data):
    """
    数据预处理
    """
    # 分离特征和标签
    X = data.drop('label', axis=1)
    y = data['label']

    # 编码标签
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)

    # 处理无穷大和 NaN
    X = X.replace([np.inf, -np.inf], np.nan)
    X = X.fillna(0)

    return X, y_encoded, label_encoder


def train_model(X_train, y_train):
    """
    训练 Random Forest 模型
    """
    print("=" * 60)
    print("训练 Random Forest 模型")
    print("=" * 60)
    print(f"参数:")
    print(f"  - 树的数量: {N_ESTIMATORS}")
    print(f"  - 最大深度: {MAX_DEPTH}")
    print(f"  - 最小分裂样本数: {MIN_SAMPLES_SPLIT}")
    print()

    model = RandomForestClassifier(
        n_estimators=N_ESTIMATORS,
        max_depth=MAX_DEPTH,
        min_samples_split=MIN_SAMPLES_SPLIT,
        random_state=RANDOM_STATE,
        n_jobs=-1,  # 使用所有CPU核心
        verbose=1
    )

    model.fit(X_train, y_train)

    return model


def evaluate_model(model, X_train, X_test, y_train, y_test, label_encoder):
    """
    评估模型性能
    """
    print("\n" + "=" * 60)
    print("模型评估")
    print("=" * 60)

    # 训练集准确率
    y_train_pred = model.predict(X_train)
    train_accuracy = accuracy_score(y_train, y_train_pred)
    print(f"\n训练集准确率: {train_accuracy:.4f} ({train_accuracy*100:.2f}%)")

    # 测试集准确率
    y_test_pred = model.predict(X_test)
    test_accuracy = accuracy_score(y_test, y_test_pred)
    print(f"测试集准确率: {test_accuracy:.4f} ({test_accuracy*100:.2f}%)")

    # 交叉验证
    cv_scores = cross_val_score(model, X_train, y_train, cv=5)
    print(f"\n5折交叉验证准确率: {cv_scores.mean():.4f} (±{cv_scores.std():.4f})")

    # F1 分数
    f1 = f1_score(y_test, y_test_pred, average='weighted')
    print(f"F1 分数 (weighted): {f1:.4f}")

    # 详细分类报告
    print("\n详细分类报告:")
    print("=" * 60)
    target_names = label_encoder.classes_
    print(classification_report(y_test, y_test_pred, target_names=target_names))

    # 混淆矩阵
    cm = confusion_matrix(y_test, y_test_pred)
    plot_confusion_matrix(cm, target_names)

    # 特征重要性
    plot_feature_importance(model, X_train.columns)

    return test_accuracy


def plot_confusion_matrix(cm, target_names):
    """
    绘制混淆矩阵
    """
    plt.figure(figsize=(10, 8))
    sns.heatmap(
        cm,
        annot=True,
        fmt='d',
        cmap='Blues',
        xticklabels=target_names,
        yticklabels=target_names
    )
    plt.title('混淆矩阵 (Confusion Matrix)')
    plt.ylabel('真实标签')
    plt.xlabel('预测标签')
    plt.tight_layout()
    plt.savefig('confusion_matrix.png', dpi=300)
    print("\n✅ 混淆矩阵已保存: confusion_matrix.png")


def plot_feature_importance(model, feature_names, top_n=20):
    """
    绘制特征重要性
    """
    importances = model.feature_importances_
    indices = np.argsort(importances)[::-1]

    # 只显示前 N 个最重要的特征
    top_indices = indices[:top_n]
    top_features = [feature_names[i] for i in top_indices]
    top_importances = importances[top_indices]

    plt.figure(figsize=(12, 8))
    plt.barh(range(len(top_features)), top_importances)
    plt.yticks(range(len(top_features)), top_features)
    plt.xlabel('重要性')
    plt.title(f'前 {top_n} 个最重要的特征')
    plt.gca().invert_yaxis()
    plt.tight_layout()
    plt.savefig('feature_importance.png', dpi=300)
    print("✅ 特征重要性已保存: feature_importance.png")

    # 打印前 10 个最重要的特征
    print("\n前 10 个最重要的特征:")
    for i in range(min(10, len(top_features))):
        print(f"  {i+1}. {top_features[i]:20s}: {top_importances[i]:.4f}")


def save_model(model, label_encoder):
    """
    保存模型和标签编码器
    """
    joblib.dump(model, MODEL_OUTPUT)
    joblib.dump(label_encoder, LABEL_ENCODER_OUTPUT)

    print(f"\n✅ 模型已保存: {MODEL_OUTPUT}")
    print(f"✅ 标签编码器已保存: {LABEL_ENCODER_OUTPUT}")

    # 显示模型文件大小
    model_size = Path(MODEL_OUTPUT).stat().st_size / 1024
    print(f"   模型文件大小: {model_size:.1f} KB")


def main():
    """
    主函数
    """
    print("\n" + "=" * 60)
    print("交通方式分类器训练")
    print("=" * 60)
    print()

    # 1. 加载数据
    data = load_data(DATA_DIR)

    # 2. 分析数据
    analyze_data(data)

    # 3. 预处理
    X, y, label_encoder = preprocess_data(data)

    # 4. 分割数据集
    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.2,
        random_state=RANDOM_STATE,
        stratify=y  # 保持类别比例
    )

    print(f"训练集大小: {len(X_train)} 条记录")
    print(f"测试集大小: {len(X_test)} 条记录\n")

    # 5. 训练模型
    model = train_model(X_train, y_train)

    # 6. 评估模型
    accuracy = evaluate_model(model, X_train, X_test, y_train, y_test, label_encoder)

    # 7. 保存模型
    save_model(model, label_encoder)

    # 总结
    print("\n" + "=" * 60)
    print("训练完成！")
    print("=" * 60)
    print(f"✅ 测试集准确率: {accuracy*100:.2f}%")
    print(f"✅ 模型文件: {MODEL_OUTPUT}")
    print(f"✅ 标签编码器: {LABEL_ENCODER_OUTPUT}")
    print("\n下一步:")
    print("1. 使用 convert_to_tflite.py 将模型转换为 TensorFlow Lite 格式")
    print("2. 将 .tflite 文件复制到 Android 项目的 assets/ 目录")
    print("3. 在 TransportModeDetector.kt 中集成模型")
    print()


if __name__ == "__main__":
    main()
