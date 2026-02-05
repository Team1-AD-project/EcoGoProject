# 导航历史记录 API 使用文档

## 概述

本文档介绍如何使用 `NavigationHistoryRepository` 来访问和管理用户完成的导航路线数据。

## 功能特性

- ✅ 自动保存每次完成的导航记录
- ✅ 支持多种查询方式（时间范围、交通方式、绿色出行等）
- ✅ 提供统计数据（总里程、减碳量等）
- ✅ 使用Room数据库，支持离线访问
- ✅ 线程安全的单例模式
- ✅ Flow支持，实时数据更新

## 快速开始

### 1. 初始化（已自动完成）

Repository已在 `EcoGoApplication` 中自动初始化，无需手动操作。

### 2. 获取Repository实例

```kotlin
import com.ecogo.app.data.repository.NavigationHistoryRepository

// 获取单例实例
val repository = NavigationHistoryRepository.getInstance()
```

### 3. 基本使用示例

#### 获取所有导航历史

```kotlin
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

// 在Activity或Fragment中
lifecycleScope.launch {
    val histories = repository.getAllHistories()
    // 处理数据
    histories.forEach { history ->
        Log.d(TAG, "从 ${history.originName} 到 ${history.destinationName}")
        Log.d(TAG, "距离: ${history.totalDistance / 1000} km")
        Log.d(TAG, "减碳: ${history.carbonSaved} kg")
    }
}
```

#### 获取最近的记录

```kotlin
lifecycleScope.launch {
    // 获取最近10条记录
    val recentHistories = repository.getRecentHistories(10)

    recentHistories.forEach { history ->
        println("${history.originName} -> ${history.destinationName}")
    }
}
```

#### 获取今天的记录

```kotlin
lifecycleScope.launch {
    val todayHistories = repository.getTodayHistories()
    Log.d(TAG, "今天完成了 ${todayHistories.size} 次出行")
}
```

#### 获取本周的记录

```kotlin
lifecycleScope.launch {
    val weekHistories = repository.getThisWeekHistories()
    Log.d(TAG, "本周完成了 ${weekHistories.size} 次出行")
}
```

#### 根据交通方式筛选

```kotlin
lifecycleScope.launch {
    // 获取所有步行记录
    val walkingHistories = repository.getHistoriesByTransportMode("walking")

    // 获取所有骑行记录
    val cyclingHistories = repository.getHistoriesByTransportMode("cycling")

    // 获取所有公交记录
    val busHistories = repository.getHistoriesByTransportMode("bus")
}
```

#### 获取绿色出行记录

```kotlin
lifecycleScope.launch {
    val greenTrips = repository.getGreenTrips()
    Log.d(TAG, "共有 ${greenTrips.size} 次绿色出行")
}
```

#### 搜索记录

```kotlin
lifecycleScope.launch {
    // 根据起点或终点名称搜索
    val results = repository.searchHistories("家")
    // 返回所有起点或终点包含"家"的记录
}
```

#### 获取统计数据

```kotlin
lifecycleScope.launch {
    val stats = repository.getStatistics()

    Log.d(TAG, "总行程数: ${stats.totalTrips}")
    Log.d(TAG, "绿色出行次数: ${stats.greenTrips}")
    Log.d(TAG, "总里程: ${stats.totalDistanceKm} km")
    Log.d(TAG, "总减碳量: ${stats.totalCarbonSavedKg} kg")
    Log.d(TAG, "绿色出行比例: ${stats.greenTripPercentage}%")
}
```

### 4. 实时数据监听（Flow）

```kotlin
import kotlinx.coroutines.flow.collect

// 监听历史记录变化
lifecycleScope.launch {
    repository.getAllHistoriesFlow().collect { histories ->
        // 每当数据库变化时，这里会自动更新
        updateUI(histories)
    }
}
```

### 5. 在ViewModel中使用

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    private val repository = NavigationHistoryRepository.getInstance()

    // 使用Flow转LiveData，自动更新UI
    val allHistories: LiveData<List<NavigationHistory>> =
        repository.getAllHistoriesFlow().asLiveData()

    // 加载统计数据
    fun loadStatistics() {
        viewModelScope.launch {
            val stats = repository.getStatistics()
            // 更新UI
        }
    }

    // 删除记录
    fun deleteHistory(historyId: Long) {
        viewModelScope.launch {
            repository.deleteHistoryById(historyId)
        }
    }
}
```

### 6. 在RecyclerView中显示

```kotlin
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater

class HistoryAdapter : RecyclerView.Adapter<HistoryViewHolder>() {

    private var histories = listOf<NavigationHistory>()

    fun submitList(newHistories: List<NavigationHistory>) {
        histories = newHistories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(histories[position])
    }

    override fun getItemCount() = histories.size
}

// 在Activity中使用
class HistoryActivity : AppCompatActivity() {

    private val adapter = HistoryAdapter()
    private val repository = NavigationHistoryRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerView.adapter = adapter

        lifecycleScope.launch {
            val histories = repository.getAllHistories()
            adapter.submitList(histories)
        }
    }
}
```

## 数据结构

### NavigationHistory

完整的导航历史记录，包含以下字段：

```kotlin
data class NavigationHistory(
    val id: Long,                          // 记录ID
    val tripId: String?,                   // 行程ID（可选）
    val userId: String?,                   // 用户ID（可选）
    val startTime: Long,                   // 开始时间（时间戳）
    val endTime: Long,                     // 结束时间（时间戳）
    val durationSeconds: Int,              // 持续时间（秒）
    val originLat: Double,                 // 起点纬度
    val originLng: Double,                 // 起点经度
    val originName: String,                // 起点名称
    val destinationLat: Double,            // 终点纬度
    val destinationLng: Double,            // 终点经度
    val destinationName: String,           // 终点名称
    val routePoints: String,               // 规划路线点（JSON）
    val trackPoints: String,               // 实际轨迹点（JSON）
    val totalDistance: Double,             // 总距离（米）
    val traveledDistance: Double,          // 实际行进距离（米）
    val transportMode: String,             // 交通方式
    val detectedMode: String?,             // AI检测的交通方式
    val totalCarbon: Double,               // 总碳排放（kg）
    val carbonSaved: Double,               // 减少的碳排放（kg）
    val isGreenTrip: Boolean,              // 是否为绿色出行
    val greenPoints: Int,                  // 绿色积分
    val routeType: String?,                // 路线类型
    val notes: String?                     // 备注
)
```

### NavigationHistorySummary

简化版本，用于列表显示：

```kotlin
data class NavigationHistorySummary(
    val id: Long,
    val startTime: Long,
    val originName: String,
    val destinationName: String,
    val totalDistance: Double,
    val transportMode: String,
    val carbonSaved: Double,
    val durationSeconds: Int
)
```

### NavigationStatistics

统计数据：

```kotlin
data class NavigationStatistics(
    val totalTrips: Int,              // 总行程数
    val greenTrips: Int,              // 绿色出行次数
    val totalDistanceMeters: Double,  // 总距离（米）
    val totalCarbonSavedKg: Double,   // 总减碳量（kg）
    val totalDistanceKm: Double,      // 总距离（公里）
    val greenTripPercentage: Double   // 绿色出行比例
)
```

## API参考

### 查询方法

| 方法 | 描述 | 返回类型 |
|------|------|----------|
| `getAllHistories()` | 获取所有历史记录 | `List<NavigationHistory>` |
| `getAllHistoriesFlow()` | 获取历史记录（Flow） | `Flow<List<NavigationHistory>>` |
| `getAllSummaries()` | 获取简化版列表 | `List<NavigationHistorySummary>` |
| `getHistoryById(id)` | 根据ID获取单条记录 | `NavigationHistory?` |
| `getRecentHistories(limit)` | 获取最近N条记录 | `List<NavigationHistory>` |
| `getTodayHistories()` | 获取今天的记录 | `List<NavigationHistory>` |
| `getThisWeekHistories()` | 获取本周的记录 | `List<NavigationHistory>` |
| `getHistoriesByTimeRange(start, end)` | 根据时间范围查询 | `List<NavigationHistory>` |
| `getHistoriesByTransportMode(mode)` | 根据交通方式查询 | `List<NavigationHistory>` |
| `getGreenTrips()` | 获取绿色出行记录 | `List<NavigationHistory>` |
| `getHistoriesByUserId(userId)` | 根据用户ID查询 | `List<NavigationHistory>` |
| `searchHistories(keyword)` | 搜索记录 | `List<NavigationHistory>` |
| `getStatistics()` | 获取统计数据 | `NavigationStatistics` |

### 修改方法

| 方法 | 描述 |
|------|------|
| `saveNavigationHistory(...)` | 保存新记录（已自动调用） |
| `updateHistory(history)` | 更新记录 |
| `deleteHistory(history)` | 删除记录 |
| `deleteHistoryById(id)` | 根据ID删除 |
| `deleteAllHistories()` | 清空所有记录 |

### 辅助方法

| 方法 | 描述 | 返回类型 |
|------|------|----------|
| `parseLatLngListFromJson(json)` | 将JSON转为LatLng列表 | `List<LatLng>` |

## 时间格式化示例

```kotlin
import java.text.SimpleDateFormat
import java.util.*

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

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
```

## 路线点解析示例

```kotlin
// 将存储的JSON字符串转换为LatLng列表
val repository = NavigationHistoryRepository.getInstance()
val history = repository.getHistoryById(1)

history?.let {
    // 解析规划路线点
    val routePoints = repository.parseLatLngListFromJson(it.routePoints)

    // 解析实际轨迹点
    val trackPoints = repository.parseLatLngListFromJson(it.trackPoints)

    // 在地图上显示
    drawRouteOnMap(routePoints)
    drawTrackOnMap(trackPoints)
}
```

## 完整示例：历史记录列表Activity

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.app.data.repository.NavigationHistoryRepository
import com.ecogo.app.databinding.ActivityNavigationHistoryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NavigationHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationHistoryBinding
    private val repository = NavigationHistoryRepository.getInstance()
    private val adapter = HistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadData()
        loadStatistics()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@NavigationHistoryActivity)
            adapter = this@NavigationHistoryActivity.adapter
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            // 使用Flow自动更新
            repository.getAllHistoriesFlow().collect { histories ->
                adapter.submitList(histories)

                // 更新标题
                binding.tvTitle.text = "历史记录 (${histories.size})"
            }
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            val stats = repository.getStatistics()

            binding.tvTotalTrips.text = "总行程: ${stats.totalTrips}"
            binding.tvGreenTrips.text = "绿色出行: ${stats.greenTrips}"
            binding.tvTotalDistance.text = String.format("总里程: %.2f km", stats.totalDistanceKm)
            binding.tvCarbonSaved.text = String.format("减碳: %.2f kg", stats.totalCarbonSavedKg)
        }
    }
}
```

## 注意事项

1. **线程安全**: 所有数据库操作必须在协程中执行，不要在主线程调用
2. **内存管理**: 使用Flow时记得在适当的时候取消收集
3. **异常处理**: 建议添加try-catch处理数据库异常
4. **数据迁移**: 当前使用`fallbackToDestructiveMigration()`，生产环境建议实现Migration策略

## 常见问题

**Q: 如何知道数据什么时候被保存？**
A: 数据在每次导航结束时自动保存（调用`stopLocationTracking()`时）

**Q: 可以手动保存记录吗？**
A: 可以，直接调用`repository.saveNavigationHistory(...)`方法

**Q: 如何导出数据？**
A: 可以通过查询所有记录，然后转换为JSON或CSV格式导出

**Q: 数据存储在哪里？**
A: 使用Room数据库，存储在应用的私有目录`/data/data/com.ecogo.app/databases/`

## 联系方式

如有问题或建议，请联系开发团队。
