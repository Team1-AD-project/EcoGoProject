/**
 * 导航历史记录 API 使用示例
 *
 * 这个文件展示了如何在你的代码中使用 NavigationHistoryRepository
 * 复制相关代码到你的项目中即可使用
 */

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ecogo.app.data.repository.NavigationHistoryRepository
import com.ecogo.app.data.local.entity.NavigationHistory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 示例 1: 获取所有历史记录
 */
fun example1GetAllHistories() {
    // 在Activity或Fragment中使用
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()
        val histories = repository.getAllHistories()

        // 遍历所有记录
        histories.forEach { history ->
            println("行程: ${history.originName} -> ${history.destinationName}")
            println("距离: ${history.totalDistance / 1000} km")
            println("减碳: ${history.carbonSaved} kg")
            println("---")
        }
    }
}

/**
 * 示例 2: 获取最近的记录
 */
fun example2GetRecentHistories() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()

        // 获取最近10条记录
        val recentHistories = repository.getRecentHistories(10)

        println("最近的 ${recentHistories.size} 条记录：")
        recentHistories.forEach { history ->
            val date = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                .format(Date(history.startTime))
            println("$date: ${history.originName} -> ${history.destinationName}")
        }
    }
}

/**
 * 示例 3: 获取今天的记录
 */
fun example3GetTodayHistories() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()
        val todayHistories = repository.getTodayHistories()

        println("今天完成了 ${todayHistories.size} 次出行")

        var totalDistance = 0.0
        var totalCarbon = 0.0

        todayHistories.forEach { history ->
            totalDistance += history.totalDistance
            totalCarbon += history.carbonSaved
        }

        println("总里程: ${totalDistance / 1000} km")
        println("总减碳: $totalCarbon kg")
    }
}

/**
 * 示例 4: 根据交通方式筛选
 */
fun example4FilterByTransportMode() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()

        // 获取所有步行记录
        val walkingHistories = repository.getHistoriesByTransportMode("walking")
        println("步行次数: ${walkingHistories.size}")

        // 获取所有骑行记录
        val cyclingHistories = repository.getHistoriesByTransportMode("cycling")
        println("骑行次数: ${cyclingHistories.size}")

        // 获取所有公交记录
        val busHistories = repository.getHistoriesByTransportMode("bus")
        println("公交次数: ${busHistories.size}")
    }
}

/**
 * 示例 5: 获取绿色出行统计
 */
fun example5GetGreenTripStats() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()

        // 获取所有绿色出行记录
        val greenTrips = repository.getGreenTrips()

        println("绿色出行次数: ${greenTrips.size}")

        val totalCarbonSaved = greenTrips.sumOf { it.carbonSaved }
        val totalDistance = greenTrips.sumOf { it.totalDistance }

        println("累计减碳: $totalCarbonSaved kg")
        println("累计里程: ${totalDistance / 1000} km")
    }
}

/**
 * 示例 6: 获取统计数据
 */
fun example6GetStatistics() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()
        val stats = repository.getStatistics()

        println("=== 统计数据 ===")
        println("总行程数: ${stats.totalTrips}")
        println("绿色出行次数: ${stats.greenTrips}")
        println("总里程: ${stats.totalDistanceKm} km")
        println("总减碳量: ${stats.totalCarbonSavedKg} kg")
        println("绿色出行比例: ${String.format("%.1f", stats.greenTripPercentage)}%")
    }
}

/**
 * 示例 7: 搜索功能
 */
fun example7SearchHistories() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()

        // 搜索包含"家"的记录
        val results = repository.searchHistories("家")

        println("找到 ${results.size} 条包含'家'的记录")
        results.forEach { history ->
            println("${history.originName} -> ${history.destinationName}")
        }
    }
}

/**
 * 示例 8: 使用Flow实时监听数据变化
 */
fun example8ObserveWithFlow() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()

        // 监听数据变化，自动更新
        repository.getAllHistoriesFlow().collect { histories ->
            println("数据已更新，共 ${histories.size} 条记录")
            // 更新UI
            updateUI(histories)
        }
    }
}

/**
 * 示例 9: 获取本周数据
 */
fun example9GetWeeklyData() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()
        val weekHistories = repository.getThisWeekHistories()

        println("=== 本周出行报告 ===")
        println("出行次数: ${weekHistories.size}")

        // 按天统计
        val dailyStats = weekHistories.groupBy { history ->
            SimpleDateFormat("MM-dd", Locale.getDefault())
                .format(Date(history.startTime))
        }

        dailyStats.forEach { (date, histories) ->
            val distance = histories.sumOf { it.totalDistance / 1000 }
            val carbon = histories.sumOf { it.carbonSaved }
            println("$date: ${histories.size}次, ${String.format("%.1f", distance)}km, 减碳${String.format("%.1f", carbon)}kg")
        }
    }
}

/**
 * 示例 10: 查看详细的单条记录
 */
fun example10GetDetailedHistory(historyId: Long) {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()
        val history = repository.getHistoryById(historyId)

        history?.let {
            println("=== 行程详情 ===")
            println("ID: ${it.id}")
            println("起点: ${it.originName}")
            println("终点: ${it.destinationName}")
            println("交通方式: ${it.transportMode}")

            if (it.detectedMode != null) {
                println("AI检测: ${it.detectedMode}")
            }

            println("总距离: ${it.totalDistance / 1000} km")
            println("实际行进: ${it.traveledDistance / 1000} km")
            println("碳排放: ${it.totalCarbon} kg")
            println("减碳量: ${it.carbonSaved} kg")
            println("绿色积分: ${it.greenPoints}")

            val duration = formatDuration(it.durationSeconds)
            println("用时: $duration")

            val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(it.startTime))
            println("开始时间: $startTime")

            // 解析路线点
            val routePoints = repository.parseLatLngListFromJson(it.routePoints)
            println("规划路线点数: ${routePoints.size}")

            val trackPoints = repository.parseLatLngListFromJson(it.trackPoints)
            println("实际轨迹点数: ${trackPoints.size}")
        } ?: println("未找到该记录")
    }
}

/**
 * 示例 11: 删除记录
 */
fun example11DeleteHistory(historyId: Long) {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()

        // 删除指定记录
        repository.deleteHistoryById(historyId)
        println("记录已删除")
    }
}

/**
 * 示例 12: 自定义时间范围查询
 */
fun example12CustomTimeRange() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()

        // 查询上个月的记录
        val calendar = Calendar.getInstance()

        // 上个月1号
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        // 上个月最后一天
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endTime = calendar.timeInMillis

        val lastMonthHistories = repository.getHistoriesByTimeRange(startTime, endTime)
        println("上个月出行 ${lastMonthHistories.size} 次")
    }
}

/**
 * 示例 13: 在RecyclerView中显示
 */
class HistoryListActivity : AppCompatActivity() {

    private lateinit var adapter: HistoryAdapter
    private val repository = NavigationHistoryRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置RecyclerView
        recyclerView.adapter = adapter

        // 加载数据
        lifecycleScope.launch {
            val histories = repository.getAllHistories()
            adapter.submitList(histories)
        }

        // 或者使用Flow自动更新
        lifecycleScope.launch {
            repository.getAllHistoriesFlow().collect { histories ->
                adapter.submitList(histories)
            }
        }
    }
}

/**
 * 示例 14: 导出数据为JSON
 */
fun example14ExportToJson() {
    lifecycleScope.launch {
        val repository = NavigationHistoryRepository.getInstance()
        val histories = repository.getAllHistories()

        // 使用Gson转换为JSON
        val gson = com.google.gson.Gson()
        val json = gson.toJson(histories)

        // 保存到文件或发送到服务器
        println("导出JSON数据:")
        println(json)
    }
}

// ========== 辅助函数 ==========

/**
 * 格式化时长
 */
fun formatDuration(durationSeconds: Int): String {
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60

    return when {
        hours > 0 -> String.format("%d小时%d分钟", hours, minutes)
        minutes > 0 -> String.format("%d分钟%d秒", minutes, seconds)
        else -> String.format("%d秒", seconds)
    }
}

/**
 * 格式化日期时间
 */
fun formatDateTime(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 更新UI（示例方法）
 */
fun updateUI(histories: List<NavigationHistory>) {
    // 在这里更新你的UI
    println("UI已更新，显示 ${histories.size} 条记录")
}

// ========== 快速使用模板 ==========

/**
 * 【模板】快速查询今天的出行情况
 */
fun quickTodayReport() {
    lifecycleScope.launch {
        val repo = NavigationHistoryRepository.getInstance()
        val today = repo.getTodayHistories()
        val stats = repo.getStatistics()

        println("""
            📊 今日出行报告
            ━━━━━━━━━━━━━━━━
            出行次数: ${today.size}
            总里程: ${String.format("%.2f", today.sumOf { it.totalDistance } / 1000)} km
            减碳量: ${String.format("%.2f", today.sumOf { it.carbonSaved })} kg

            🌍 累计统计
            ━━━━━━━━━━━━━━━━
            总行程: ${stats.totalTrips}
            绿色出行: ${stats.greenTrips} (${String.format("%.1f", stats.greenTripPercentage)}%)
            累计减碳: ${String.format("%.2f", stats.totalCarbonSavedKg)} kg
        """.trimIndent())
    }
}

/**
 * 【模板】快速获取最新记录的详情
 */
fun quickGetLatest() {
    lifecycleScope.launch {
        val repo = NavigationHistoryRepository.getInstance()
        val latest = repo.getRecentHistories(1).firstOrNull()

        latest?.let {
            println("最新行程: ${it.originName} -> ${it.destinationName}")
            println("距离: ${String.format("%.2f", it.totalDistance / 1000)} km")
            println("减碳: ${String.format("%.2f", it.carbonSaved)} kg")
        } ?: println("暂无记录")
    }
}
