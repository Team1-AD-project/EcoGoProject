#!/usr/bin/env python3
"""
TensorFlow Lite 交通方式分类模型训练脚本

用途：
1. 加载从Android应用导出的训练数据 (CSV格式)
2. 提取特征向量和标签
3. 训练神经网络模型
4. 验证模型性能
5. 转换为TensorFlow Lite格式
6. 生成model.tflite文件供Android使用

使用方法：
    python3 train_model.py --data labeled_journeys.csv --output model.tflite

依赖：
    pip install tensorflow numpy pandas scikit-learn matplotlib
"""

import argparse
import csv
import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Tuple, List, Dict

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import confusion_matrix, classification_report, accuracy_score
import matplotlib.pyplot as plt

# 日志配置
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 常数定义
TRANSPORT_MODES = ["WALKING", "CYCLING", "BUS", "DRIVING", "SUBWAY"]
INPUT_FEATURES = 17  # 特征向量维度 (14传感器 + 3 GPS速度)
OUTPUT_CLASSES = 5   # 交通方式类别数

# 特征列名 (需要与Android FeatureExtractor匹配)
FEATURE_COLUMNS = [
    # 加速度计特征 (7)
    'accelMeanX', 'accelMeanY', 'accelMeanZ', 'accelStdX', 'accelStdY', 'accelStdZ',
    'accelMagnitude',
    # 陀螺仪特征 (6)
    'gyroMeanX', 'gyroMeanY', 'gyroMeanZ', 'gyroStdX', 'gyroStdY', 'gyroStdZ',
    # 时间特征 (1)
    'journeyDuration',
    # GPS速度特征 (3)
    'gpsSpeedMean', 'gpsSpeedStd', 'gpsSpeedMax',
]

TARGET_COLUMN = 'transportMode'


def load_data(csv_file: str) -> Tuple[np.ndarray, np.ndarray]:
    """
    从CSV文件加载训练数据
    
    Args:
        csv_file: CSV文件路径
        
    Returns:
        (特征数组, 标签数组)
    """
    logger.info(f"从文件加载数据: {csv_file}")
    
    df = pd.read_csv(csv_file)
    logger.info(f"总记录数: {len(df)}")
    
    # 统计各交通方式的样本数
    mode_counts = df[TARGET_COLUMN].value_counts()
    logger.info("交通方式分布:")
    for mode, count in mode_counts.items():
        logger.info(f"  {mode}: {count} 个")
    
    # 检查缺失值
    missing = df[FEATURE_COLUMNS + [TARGET_COLUMN]].isnull().sum()
    if missing.any():
        logger.warning(f"检测到缺失值:\n{missing[missing > 0]}")
        df = df.dropna(subset=FEATURE_COLUMNS + [TARGET_COLUMN])
        logger.info(f"删除缺失值后: {len(df)} 条记录")
    
    # 提取特征和标签
    X = df[FEATURE_COLUMNS].values.astype(np.float32)
    
    # 标签编码
    y = np.array([TRANSPORT_MODES.index(mode) for mode in df[TARGET_COLUMN]])
    
    logger.info(f"特征形状: {X.shape}")
    logger.info(f"标签形状: {y.shape}")
    
    return X, y


def preprocess_features(X_train: np.ndarray, X_test: np.ndarray) -> Tuple[np.ndarray, np.ndarray, StandardScaler]:
    """
    标准化特征 (Z-score归一化)
    
    Args:
        X_train: 训练数据特征
        X_test: 测试数据特征
        
    Returns:
        (标准化训练数据, 标准化测试数据, Scaler对象)
    """
    logger.info("正在标准化特征...")
    
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    
    logger.info(f"训练数据统计: 均值={X_train_scaled.mean():.4f}, 标准差={X_train_scaled.std():.4f}")
    
    return X_train_scaled, X_test_scaled, scaler


def build_model() -> tf.keras.Model:
    """
    构建神经网络模型
    
    模型架构：
    - 输入层：34个特征
    - 隐藏层1：64个神经元，ReLU激活
    - Dropout：0.3 (防止过拟合)
    - 隐藏层2：32个神经元，ReLU激活
    - Dropout：0.2
    - 隐藏层3：16个神经元，ReLU激活
    - 输出层：5个神经元，Softmax激活
    
    Returns:
        编译后的Keras模型
    """
    logger.info("构建模型...")
    
    model = tf.keras.Sequential([
        tf.keras.layers.InputLayer(shape=(INPUT_FEATURES,)),
        
        # 第一层：64个神经元
        tf.keras.layers.Dense(64, activation='relu', name='dense_1'),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.3),
        
        # 第二层：32个神经元
        tf.keras.layers.Dense(32, activation='relu', name='dense_2'),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.2),
        
        # 第三层：16个神经元
        tf.keras.layers.Dense(16, activation='relu', name='dense_3'),
        tf.keras.layers.Dropout(0.1),
        
        # 输出层：5个交通方式
        tf.keras.layers.Dense(OUTPUT_CLASSES, activation='softmax', name='output')
    ])
    
    # 编译模型
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    logger.info("模型架构:")
    model.summary(print_fn=logger.info)
    
    return model


def train_model(
    model: tf.keras.Model,
    X_train: np.ndarray,
    y_train: np.ndarray,
    X_val: np.ndarray,
    y_val: np.ndarray,
    epochs: int = 100,
    batch_size: int = 16
) -> tf.keras.callbacks.History:
    """
    训练模型
    
    Args:
        model: Keras模型
        X_train: 训练特征
        y_train: 训练标签
        X_val: 验证特征
        y_val: 验证标签
        epochs: 训练轮数
        batch_size: 批大小
        
    Returns:
        训练历史对象
    """
    logger.info(f"开始训练 (epochs={epochs}, batch_size={batch_size})...")

    # 回调函数：提前停止和学习率衰减
    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor='val_loss',
            patience=15,
            restore_best_weights=True,
            verbose=1
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            min_lr=0.00001,
            verbose=1
        )
    ]

    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=epochs,
        batch_size=batch_size,
        callbacks=callbacks,
        verbose=1
    )
    
    return history


def evaluate_model(
    model: tf.keras.Model,
    X_test: np.ndarray,
    y_test: np.ndarray
) -> Dict:
    """
    评估模型性能
    
    Args:
        model: 训练完成的Keras模型
        X_test: 测试特征
        y_test: 测试标签
        
    Returns:
        包含精度、召回、混淆矩阵等的评估结果
    """
    logger.info("评估模型...")
    
    # 预测
    y_pred_prob = model.predict(X_test, verbose=0)
    y_pred = np.argmax(y_pred_prob, axis=1)
    
    # 计算指标
    accuracy = accuracy_score(y_test, y_pred)
    logger.info(f"测试精度: {accuracy:.4f}")
    
    # 分类报告
    logger.info("分类报告:")
    report = classification_report(
        y_test, y_pred,
        target_names=TRANSPORT_MODES,
        digits=4
    )
    logger.info(report)
    
    # 混淆矩阵
    cm = confusion_matrix(y_test, y_pred)
    logger.info(f"混淆矩阵:\n{cm}")
    
    return {
        'accuracy': accuracy,
        'confusion_matrix': cm.tolist(),
        'report': report
    }


def save_model_as_tflite(
    model: tf.keras.Model,
    output_path: str,
    representative_data: np.ndarray = None
) -> bool:
    """
    将Keras模型转换为TensorFlow Lite格式
    
    Args:
        model: Keras模型
        output_path: 输出.tflite文件的路径
        representative_data: 代表性数据（用于量化，可选）
        
    Returns:
        转换成功则返回True
    """
    logger.info(f"转换模型为TensorFlow Lite格式...")
    
    try:
        # 先导出为SavedModel格式（兼容Keras 3）
        import tempfile
        saved_model_dir = tempfile.mkdtemp()
        model.export(saved_model_dir)

        # 从SavedModel创建转换器
        converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)

        # 设置转换选项
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS
        ]

        # 转换
        tflite_model = converter.convert()
        
        # 保存
        Path(output_path).parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, 'wb') as f:
            f.write(tflite_model)
        
        logger.info(f"模型已保存: {output_path}")
        logger.info(f"模型文件大小: {len(tflite_model) / 1024:.2f} KB")
        
        return True
    except Exception as e:
        logger.error(f"转换失败: {e}")
        return False


def plot_training_history(history: tf.keras.callbacks.History, output_dir: str = '.'):
    """
    绘制训练历史图表
    """
    try:
        fig, axes = plt.subplots(1, 2, figsize=(12, 4))
        
        # 精度曲线
        axes[0].plot(history.history['accuracy'], label='训练精度')
        axes[0].plot(history.history['val_accuracy'], label='验证精度')
        axes[0].set_xlabel('Epoch')
        axes[0].set_ylabel('Accuracy')
        axes[0].set_title('模型精度')
        axes[0].legend()
        axes[0].grid(True)
        
        # 损失曲线
        axes[1].plot(history.history['loss'], label='训练损失')
        axes[1].plot(history.history['val_loss'], label='验证损失')
        axes[1].set_xlabel('Epoch')
        axes[1].set_ylabel('Loss')
        axes[1].set_title('模型损失')
        axes[1].legend()
        axes[1].grid(True)
        
        plt.tight_layout()
        output_path = Path(output_dir) / 'training_history.png'
        plt.savefig(output_path)
        logger.info(f"训练历史图表已保存: {output_path}")
        
    except Exception as e:
        logger.warning(f"无法绘制图表: {e}")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='训练交通方式分类模型')
    parser.add_argument('--data', required=True, help='训练数据CSV文件路径')
    parser.add_argument('--output', default='model.tflite', help='输出TLite模型文件路径')
    parser.add_argument('--epochs', type=int, default=100, help='训练轮数')
    parser.add_argument('--batch-size', type=int, default=16, help='批大小')
    parser.add_argument('--test-size', type=float, default=0.2, help='测试集比例')
    parser.add_argument('--val-size', type=float, default=0.1, help='验证集比例')
    
    args = parser.parse_args()
    
    logger.info("=" * 60)
    logger.info("交通方式分类模型训练")
    logger.info(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    logger.info("=" * 60)
    
    # 步骤1：加载数据
    if not Path(args.data).exists():
        logger.error(f"文件不存在: {args.data}")
        return
    
    X, y = load_data(args.data)
    
    # 步骤2：划分训练/验证/测试集
    logger.info("划分数据集...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=args.test_size, random_state=42, stratify=y
    )
    
    X_train, X_val, y_train, y_val = train_test_split(
        X_train, y_train, test_size=args.val_size, random_state=42, stratify=y_train
    )
    
    logger.info(f"训练集: {X_train.shape[0]}")
    logger.info(f"验证集: {X_val.shape[0]}")
    logger.info(f"测试集: {X_test.shape[0]}")
    
    # 步骤3：标准化特征
    X_train_scaled, X_test_scaled, scaler = preprocess_features(X_train, X_test)
    X_val_scaled = scaler.transform(X_val)
    
    # 步骤4：构建和训练模型
    model = build_model()
    history = train_model(
        model, X_train_scaled, y_train, X_val_scaled, y_val,
        epochs=args.epochs, batch_size=args.batch_size
    )
    
    # 步骤5：评估模型
    results = evaluate_model(model, X_test_scaled, y_test)
    
    # 步骤6：转换为TensorFlow Lite
    success = save_model_as_tflite(model, args.output, X_train_scaled)
    
    if success:
        logger.info("=" * 60)
        logger.info(f"模型训练完成!")
        logger.info(f"- TFLite文件: {args.output}")
        logger.info(f"- 测试精度: {results['accuracy']:.2%}")
        logger.info(f"- 输出文件应放在: app/src/main/assets/")
        logger.info("=" * 60)
        
        # 绘制训练图表
        plot_training_history(history, Path(args.output).parent)
    else:
        logger.error("模型转换失败")


if __name__ == '__main__':
    main()
