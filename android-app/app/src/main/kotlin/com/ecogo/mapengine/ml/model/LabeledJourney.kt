package com.ecogo.mapengine.ml.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 标记的出行数据 - 用于ML训练
 * 包含GPS轨迹、传感器数据和用户验证的交通方式标签
 */
@Entity(tableName = "labeled_journeys")
data class LabeledJourney(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 基本信息
    val startTime: Long,           // 出行开始时间 (ms)
    val endTime: Long,             // 出行结束时间 (ms)
    val transportMode: String,     // 交通方式标签 (WALKING, CYCLING, BUS, DRIVING, SUBWAY)
    val labelSource: String,       // 标签来源 (AUTO_SNAP, MANUAL, VERIFIED)
    
    // GPS轨迹 (JSON格式存储)
    val gpsTrajectory: String,     // [{"lat":30.1,"lng":120.1,"time":1234567890},...]
    val gpsPointCount: Int,        // GPS点数
    
    // 速度统计
    val avgSpeed: Float,           // 平均速度 (m/s)
    val maxSpeed: Float,           // 最大速度 (m/s)
    val minSpeed: Float,           // 最小速度 (m/s)
    val speedVariance: Float,      // 速度方差
    
    // 传感器数据 (JSON格式存储)
    val accelerometerData: String, // 加速度计原始数据
    val gyroscopeData: String,     // 陀螺仪原始数据
    val barometerData: String,     // 气压计原始数据 (海拔变化)
    
    // 从Snap to Roads获得的信息
    val roadTypes: String,         // 道路类型 (motorway,trunk,primary,...)
    val snapConfidence: Float,     // Snap to Roads返回的匹配置信度
    
    // 质量指标
    val gpsAccuracy: Float,        // GPS精度 (meters) - 平均值
    val isVerified: Boolean = false,  // 是否被人工验证
    val verificationTime: Long? = null, // 验证时间
    val verificationNotes: String = "" // 验证备注
)

/**
 * 用于导出到Python的特征向量数据结构
 */
data class JourneyFeatures(
    // 加速度计特征 (x,y,z轴)
    val accelMeanX: Float,
    val accelMeanY: Float,
    val accelMeanZ: Float,
    val accelStdX: Float,
    val accelStdY: Float,
    val accelStdZ: Float,
    val accelMagnitude: Float,

    // 陀螺仪特征 (x,y,z轴)
    val gyroMeanX: Float,
    val gyroMeanY: Float,
    val gyroMeanZ: Float,
    val gyroStdX: Float,
    val gyroStdY: Float,
    val gyroStdZ: Float,

    // 持续时间特征
    val journeyDuration: Float,    // 秒

    // GPS速度特征
    val gpsSpeedMean: Float = 0f,  // GPS平均速度 (m/s)
    val gpsSpeedStd: Float = 0f,   // GPS速度标准差
    val gpsSpeedMax: Float = 0f,   // GPS最大速度

    // 目标标签
    val transportMode: String      // WALKING, CYCLING, BUS, DRIVING, SUBWAY
)

/**
 * 用于导出CSV的结构 - 包含全部17个特征 + 元数据
 */
data class JourneyCSVRecord(
    val journeyId: Long,
    val timestamp: Long,
    val transportMode: String,
    val labelSource: String,
    // 加速度计特征 (7)
    val accelMeanX: Float,
    val accelMeanY: Float,
    val accelMeanZ: Float,
    val accelStdX: Float,
    val accelStdY: Float,
    val accelStdZ: Float,
    val accelMagnitude: Float,
    // 陀螺仪特征 (6)
    val gyroMeanX: Float,
    val gyroMeanY: Float,
    val gyroMeanZ: Float,
    val gyroStdX: Float,
    val gyroStdY: Float,
    val gyroStdZ: Float,
    // 时间特征 (1)
    val journeyDuration: Float,
    // GPS速度特征 (3)
    val gpsSpeedMean: Float,
    val gpsSpeedStd: Float,
    val gpsSpeedMax: Float,
    val isVerified: Boolean
)
