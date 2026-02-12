package com.ecogo.app.ui.ml

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ecogo.app.databinding.ActivityDataCollectionBinding
import com.ecogo.app.ml.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

/**
 * 数据收集界面
 * 用于收集机器学习训练数据
 */
class DataCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataCollectionBinding
    private lateinit var sensorCollector: SensorDataCollector
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var isRecording = false
    private var currentLabel = TransportModeLabel.WALKING
    private var recordingStartTime = 0L

    // 存储收集的数据窗口
    private val collectedWindows = mutableListOf<Pair<SensorWindow, TransportModeLabel>>()

    // 统计每个类别收集的数据量
    private val labelCounts = mutableMapOf<TransportModeLabel, Int>()

    // 计时器
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateRecordingTime()
            timerHandler.postDelayed(this, 1000)
        }
    }

    // 位置回调
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                sensorCollector.updateGpsSpeed(location)
            }
        }
    }

    // 位置权限请求
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                startLocationUpdates()
            }
            else -> {
                Toast.makeText(this, "需要位置权限来记录GPS速度", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化
        sensorCollector = SensorDataCollector(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        checkPermissions()

        // 监听数据窗口
        lifecycleScope.launch {
            sensorCollector.windowFlow.collect { window ->
                window?.let {
                    onWindowCollected(it)
                }
            }
        }
    }

    private fun setupUI() {
        // 交通方式选择
        binding.chipGroupTransportMode.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            currentLabel = when (checkedIds.first()) {
                binding.chipWalking.id -> TransportModeLabel.WALKING
                binding.chipCycling.id -> TransportModeLabel.CYCLING
                binding.chipBus.id -> TransportModeLabel.BUS
                binding.chipSubway.id -> TransportModeLabel.SUBWAY
                binding.chipDriving.id -> TransportModeLabel.DRIVING
                else -> TransportModeLabel.WALKING
            }
        }

        // 开始/停止按钮
        binding.btnStartStop.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // 导出数据按钮
        binding.btnExportData.setOnClickListener {
            exportData()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000  // 每秒更新一次
        ).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun startRecording() {
        isRecording = true
        recordingStartTime = SystemClock.elapsedRealtime()

        // 开始采集传感器数据
        sensorCollector.startCollecting()

        // 启动计时器
        timerHandler.post(timerRunnable)

        // 更新 UI
        binding.btnStartStop.text = "停止记录"
        binding.btnStartStop.backgroundTintList =
            ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        binding.chipGroupTransportMode.isEnabled = false
        binding.tvRecordingStatus.text = "正在记录 ${currentLabel.displayName()}"

        Toast.makeText(this, "开始记录 ${currentLabel.displayName()}", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        isRecording = false

        // 停止采集
        sensorCollector.stopCollecting()

        // 停止计时器
        timerHandler.removeCallbacks(timerRunnable)

        // 更新 UI
        binding.btnStartStop.text = "开始记录"
        binding.btnStartStop.backgroundTintList =
            ContextCompat.getColorStateList(this, com.ecogo.app.R.color.green_primary)
        binding.chipGroupTransportMode.isEnabled = true
        binding.tvRecordingStatus.text = "已停止"

        // 启用导出按钮
        if (collectedWindows.isNotEmpty()) {
            binding.btnExportData.isEnabled = true
        }

        Toast.makeText(this, "已停止记录", Toast.LENGTH_SHORT).show()
    }

    private fun updateRecordingTime() {
        if (!isRecording) return

        val elapsed = SystemClock.elapsedRealtime() - recordingStartTime
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / 1000 / 60) % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)

        binding.tvRecordingTime.text = "记录时长: $timeStr"
    }

    private fun onWindowCollected(window: SensorWindow) {
        if (!isRecording) return

        // 保存窗口和标签
        collectedWindows.add(window to currentLabel)

        // 更新统计
        labelCounts[currentLabel] = (labelCounts[currentLabel] ?: 0) + 1

        // 更新 UI
        runOnUiThread {
            binding.tvWindowCount.text = "已收集窗口: ${collectedWindows.size}"
            updateDataStatistics()
        }
    }

    private fun updateDataStatistics() {
        val stats = buildString {
            TransportModeLabel.values().filter { it != TransportModeLabel.UNKNOWN }.forEach { label ->
                val count = labelCounts[label] ?: 0
                val minutes = count * 2.5 / 60  // 每个窗口 2.5 秒
                appendLine("${label.displayName()}: $count 个窗口 (约 ${String.format("%.1f", minutes)} 分钟)")
            }
        }
        binding.tvDataStatistics.text = stats.trimEnd()
    }

    private fun exportData() {
        if (collectedWindows.isEmpty()) {
            Toast.makeText(this, "没有数据可导出", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    exportToCSV()
                }
                Toast.makeText(
                    this@DataCollectionActivity,
                    "数据已导出到: ${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@DataCollectionActivity,
                    "导出失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun exportToCSV(): File {
        // 创建输出文件
        val outputDir = File(getExternalFilesDir(null), "ml_training_data")
        outputDir.mkdirs()

        val timestamp = System.currentTimeMillis()
        val file = File(outputDir, "sensor_data_$timestamp.csv")

        FileWriter(file).use { writer ->
            // 写入表头
            writer.append("accXMean,accXStd,accXMax,accXMin,accXRange,accXMedian,accXSma,")
            writer.append("accYMean,accYStd,accYMax,accYMin,accYRange,accYMedian,accYSma,")
            writer.append("accZMean,accZStd,accZMax,accZMin,accZRange,accZMedian,accZSma,")
            writer.append("gyroXMean,gyroXStd,gyroXMax,gyroXMin,gyroXRange,gyroXMedian,gyroXSma,")
            writer.append("gyroYMean,gyroYStd,gyroYMax,gyroYMin,gyroYRange,gyroYMedian,gyroYSma,")
            writer.append("gyroZMean,gyroZStd,gyroZMax,gyroZMin,gyroZRange,gyroZMedian,gyroZSma,")
            writer.append("accMagnitudeMean,accMagnitudeStd,accMagnitudeMax,")
            writer.append("gyroMagnitudeMean,gyroMagnitudeStd,gyroMagnitudeMax,")
            writer.append("gpsSpeedMean,gpsSpeedStd,gpsSpeedMax,")
            writer.append("pressureMean,pressureStd,")
            writer.append("label\n")

            // 写入数据
            collectedWindows.forEach { (window, label) ->
                val features = SensorFeatureExtractor.extractFeatures(window)
                val featureArray = features.toFloatArray()

                featureArray.forEach { value ->
                    writer.append("$value,")
                }
                writer.append("${label.name}\n")
            }
        }

        return file
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording()
        }
        sensorCollector.cleanup()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

// 扩展函数
fun TransportModeLabel.displayName(): String {
    return when (this) {
        TransportModeLabel.WALKING -> "步行"
        TransportModeLabel.CYCLING -> "骑行"
        TransportModeLabel.BUS -> "公交"
        TransportModeLabel.SUBWAY -> "地铁"
        TransportModeLabel.DRIVING -> "驾车"
        TransportModeLabel.UNKNOWN -> "未知"
    }
}
