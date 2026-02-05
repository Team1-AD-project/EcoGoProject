# 在地图上显示历史路径 - 完整指南

## 📊 数据结构说明

每条导航历史记录包含两种路径数据：

### 1. **routePoints** - 规划路线（JSON字符串）
```json
[
  {"lat": 23.123456, "lng": 113.234567},
  {"lat": 23.123789, "lng": 113.234890},
  ...
]
```
- 📏 **点数**: 通常 50-200 个点
- 🎯 **用途**: 显示规划的推荐路线
- ⚡ **性能**: 较少，加载快
- 🎨 **建议样式**: 蓝色虚线

### 2. **trackPoints** - 实际GPS轨迹（JSON字符串）
```json
[
  {"lat": 23.123450, "lng": 113.234560},
  {"lat": 23.123455, "lng": 113.234565},
  ...
]
```
- 📏 **点数**: 可能 500-3000+ 个点
- 🎯 **用途**: 显示用户实际走过的路径
- ⚡ **性能**: 较多，需要优化
- 🎨 **建议样式**: 绿色实线

---

## 🚀 方案一：最简单的实现（适合快速集成）

### 步骤 1: 获取单条历史记录

```kotlin
import androidx.lifecycle.lifecycleScope
import com.ecogo.app.data.repository.NavigationHistoryRepository
import kotlinx.coroutines.launch

lifecycleScope.launch {
    val repository = NavigationHistoryRepository.getInstance()

    // 获取最新的一条记录
    val latestHistory = repository.getRecentHistories(1).firstOrNull()

    latestHistory?.let { history ->
        // 解析路径点（使用规划路线，点数少）
        val routePoints = repository.parseLatLngListFromJson(history.routePoints)

        // 在地图上绘制
        drawRouteOnMap(routePoints)
    }
}
```

### 步骤 2: 在地图上绘制

```kotlin
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*

fun drawRouteOnMap(points: List<LatLng>) {
    if (points.isEmpty()) return

    // 绘制路线
    val polyline = googleMap?.addPolyline(
        PolylineOptions()
            .addAll(points)
            .width(10f)
            .color(Color.BLUE)
            .geodesic(true)
    )

    // 添加起点和终点标记
    googleMap?.addMarker(
        MarkerOptions()
            .position(points.first())
            .title("起点")
    )

    googleMap?.addMarker(
        MarkerOptions()
            .position(points.last())
            .title("终点")
    )

    // 调整视角显示完整路线
    fitMapToRoute(points)
}

fun fitMapToRoute(points: List<LatLng>) {
    if (points.isEmpty()) return

    val boundsBuilder = LatLngBounds.Builder()
    points.forEach { boundsBuilder.include(it) }
    val bounds = boundsBuilder.build()

    googleMap?.animateCamera(
        CameraUpdateFactory.newLatLngBounds(bounds, 100)
    )
}
```

---

## ⚡ 方案二：优化版（数据量大时推荐）

### 使用路径简化工具

```kotlin
import com.ecogo.app.util.RouteSimplifier

lifecycleScope.launch {
    val repository = NavigationHistoryRepository.getInstance()
    val history = repository.getRecentHistories(1).firstOrNull()

    history?.let {
        // 解析实际轨迹（可能有上千个点）
        val trackPoints = repository.parseLatLngListFromJson(it.trackPoints)

        // 🔥 方法1: 使用智能算法简化（推荐）
        val simplifiedPoints = RouteSimplifier.simplify(
            points = trackPoints,
            tolerance = 20.0  // 容差20米，值越大简化越多
        )

        // 🔥 方法2: 简化到指定点数
        // val simplifiedPoints = RouteSimplifier.simplifyToCount(trackPoints, 100)

        // 🔥 方法3: 按间隔抽样
        // val simplifiedPoints = RouteSimplifier.simplifyByInterval(trackPoints, 10)

        println("原始点数: ${trackPoints.size}")
        println("简化后点数: ${simplifiedPoints.size}")

        // 绘制简化后的路径
        drawRouteOnMap(simplifiedPoints)
    }
}
```

### 简化效果对比

| 原始点数 | 简化后点数 | 数据减少 | 效果 |
|---------|-----------|---------|------|
| 2000点  | 100-200点 | 90%     | 几乎无视觉差异 |
| 1000点  | 50-100点  | 90%     | 轻微简化 |
| 500点   | 40-80点   | 85%     | 保持原貌 |

---

## 🎨 方案三：完整展示（同时显示规划和实际）

```kotlin
lifecycleScope.launch {
    val repository = NavigationHistoryRepository.getInstance()
    val history = repository.getHistoryById(historyId)

    history?.let {
        // 1. 规划路线（蓝色虚线）
        val plannedRoute = repository.parseLatLngListFromJson(it.routePoints)
        drawPlannedRoute(plannedRoute)

        // 2. 实际轨迹（绿色实线，简化后）
        val actualTrack = repository.parseLatLngListFromJson(it.trackPoints)
        val simplifiedTrack = RouteSimplifier.simplify(actualTrack, tolerance = 20.0)
        drawActualTrack(simplifiedTrack)

        // 3. 添加起点和终点标记
        addMarkers(
            origin = LatLng(it.originLat, it.originLng),
            destination = LatLng(it.destinationLat, it.destinationLng),
            originName = it.originName,
            destinationName = it.destinationName
        )
    }
}

// 绘制规划路线（蓝色虚线）
fun drawPlannedRoute(points: List<LatLng>) {
    googleMap?.addPolyline(
        PolylineOptions()
            .addAll(points)
            .width(8f)
            .color(Color.parseColor("#4285F4"))  // 蓝色
            .pattern(listOf(Dash(20f), Gap(10f))) // 虚线
            .geodesic(true)
    )
}

// 绘制实际轨迹（绿色实线）
fun drawActualTrack(points: List<LatLng>) {
    googleMap?.addPolyline(
        PolylineOptions()
            .addAll(points)
            .width(10f)
            .color(Color.parseColor("#4CAF50"))  // 绿色
            .geodesic(true)
    )
}

// 添加标记
fun addMarkers(
    origin: LatLng,
    destination: LatLng,
    originName: String,
    destinationName: String
) {
    googleMap?.addMarker(
        MarkerOptions()
            .position(origin)
            .title(originName)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
    )

    googleMap?.addMarker(
        MarkerOptions()
            .position(destination)
            .title(destinationName)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
    )
}
```

---

## 📱 方案四：使用专用Activity（最完整）

### 打开历史地图页面

```kotlin
import android.content.Intent
import com.ecogo.app.ui.history.HistoryMapActivity

// 方式1: 传入历史记录ID
val intent = Intent(this, HistoryMapActivity::class.java)
intent.putExtra("HISTORY_ID", historyId)
startActivity(intent)

// 方式2: 从列表点击进入
class HistoryAdapter : RecyclerView.Adapter<ViewHolder>() {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = histories[position]
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, HistoryMapActivity::class.java)
            intent.putExtra("HISTORY_ID", history.id)
            holder.itemView.context.startActivity(intent)
        }
    }
}
```

这个Activity已经实现了：
- ✅ 自动加载历史记录
- ✅ 显示规划路线和实际轨迹
- ✅ 切换显示/隐藏
- ✅ 显示详细信息（距离、时间、减碳等）
- ✅ 起点终点标记

---

## 📋 方案五：获取多条记录（列表展示）

### 获取最近的记录

```kotlin
lifecycleScope.launch {
    val repository = NavigationHistoryRepository.getInstance()

    // 获取最近10条记录
    val recentHistories = repository.getRecentHistories(10)

    recentHistories.forEach { history ->
        println("""
            ${history.originName} → ${history.destinationName}
            距离: ${String.format("%.2f", history.totalDistance / 1000)} km
            减碳: ${String.format("%.2f", history.carbonSaved)} kg
        """.trimIndent())
    }
}
```

### 获取今天的记录

```kotlin
lifecycleScope.launch {
    val todayHistories = repository.getTodayHistories()

    // 在地图上依次显示今天的所有路径
    todayHistories.forEach { history ->
        val points = repository.parseLatLngListFromJson(history.trackPoints)
        val simplified = RouteSimplifier.simplifyToCount(points, 50) // 每条路径最多50个点
        drawRouteOnMap(simplified)
    }
}
```

---

## 🎯 最佳实践建议

### 1. 根据使用场景选择数据源

| 场景 | 推荐数据源 | 原因 |
|------|-----------|------|
| 列表预览缩略图 | `routePoints` | 点数少，加载快 |
| 详细查看 | `trackPoints` + 简化 | 更精确，需优化 |
| 统计分析 | `routePoints` | 足够准确，性能好 |
| 路径对比 | 两者都用 | 完整信息 |

### 2. 性能优化建议

```kotlin
// ✅ 好的做法：异步加载 + 简化
lifecycleScope.launch {
    val points = repository.parseLatLngListFromJson(history.trackPoints)
    val simplified = RouteSimplifier.simplify(points, 20.0)

    withContext(Dispatchers.Main) {
        drawRouteOnMap(simplified)
    }
}

// ❌ 避免：直接在主线程绘制大量点
val points = repository.parseLatLngListFromJson(history.trackPoints) // 可能有3000个点
drawRouteOnMap(points) // 会卡顿
```

### 3. 内存管理

```kotlin
// 处理大量历史记录时，使用分页加载
lifecycleScope.launch {
    // 每次只加载10条
    val page1 = repository.getRecentHistories(10)
    displayHistories(page1)

    // 用户滚动时再加载更多
    // ...
}
```

### 4. 推荐的简化参数

```kotlin
// 不同场景的推荐参数
val tolerance = when (useCase) {
    UseCase.LIST_PREVIEW -> 50.0      // 列表预览：高度简化
    UseCase.NORMAL_VIEW -> 20.0       // 普通查看：中等简化
    UseCase.DETAILED_VIEW -> 10.0     // 详细查看：轻度简化
    UseCase.ANALYSIS -> 5.0           // 数据分析：几乎不简化
}

val simplified = RouteSimplifier.simplify(points, tolerance)
```

---

## 🔧 完整示例：在你的Activity中使用

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ecogo.app.data.repository.NavigationHistoryRepository
import com.ecogo.app.util.RouteSimplifier
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.coroutines.launch

class YourMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val repository = NavigationHistoryRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.your_layout)

        // 初始化地图
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        loadAndDisplayRoute()
    }

    private fun loadAndDisplayRoute() {
        lifecycleScope.launch {
            // 1. 获取历史记录
            val history = repository.getRecentHistories(1).firstOrNull() ?: return@launch

            // 2. 解析路径点（优先使用规划路线，数据量小）
            val points = repository.parseLatLngListFromJson(history.routePoints)

            // 3. 如果需要更精确的轨迹，使用实际GPS轨迹并简化
            // val trackPoints = repository.parseLatLngListFromJson(history.trackPoints)
            // val points = RouteSimplifier.simplify(trackPoints, tolerance = 20.0)

            // 4. 绘制路线
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(10f)
                    .color(Color.BLUE)
                    .geodesic(true)
            )

            // 5. 调整相机
            if (points.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.Builder()
                points.forEach { boundsBuilder.include(it) }
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
                )
            }
        }
    }
}
```

---

## 📊 数据量对比

### 示例数据分析

假设一次10公里的骑行：

| 数据类型 | 点数 | JSON大小 | 建议用途 |
|---------|------|---------|---------|
| routePoints | 80个点 | ~3KB | ✅ 默认使用 |
| trackPoints（原始） | 2000个点 | ~80KB | ❌ 需要简化 |
| trackPoints（简化后） | 120个点 | ~5KB | ✅ 推荐使用 |

### 简化前后对比

```kotlin
// 打印统计信息
val original = repository.parseLatLngListFromJson(history.trackPoints)
val simplified = RouteSimplifier.simplify(original, 20.0)

val originalStats = RouteSimplifier.getRouteStats(original)
val simplifiedStats = RouteSimplifier.getRouteStats(simplified)

println("""
    原始数据:
    - 点数: ${originalStats.pointCount}
    - 距离: ${String.format("%.2f", originalStats.totalDistanceKm)} km

    简化后:
    - 点数: ${simplifiedStats.pointCount}
    - 距离: ${String.format("%.2f", simplifiedStats.totalDistanceKm)} km
    - 数据减少: ${(1 - simplifiedStats.pointCount.toFloat() / originalStats.pointCount) * 100}%
""".trimIndent())
```

---

## 🎨 视觉效果建议

### 推荐的颜色方案

```kotlin
// 规划路线：蓝色虚线
Color.parseColor("#4285F4")  // Google 蓝

// 实际轨迹：绿色实线
Color.parseColor("#4CAF50")  // 环保绿

// 起点标记：绿色
BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

// 终点标记：红色
BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
```

### 路线宽度建议

```kotlin
when (routeType) {
    RouteType.PLANNED -> 8f      // 规划路线：较细
    RouteType.ACTUAL -> 10f      // 实际轨迹：较粗
    RouteType.PREVIEW -> 6f      // 预览模式：最细
}
```

---

## ❓ 常见问题

### Q1: 数据量太大怎么办？
**A**: 使用 `RouteSimplifier.simplify()` 简化路径，推荐tolerance=20.0

### Q2: 如何选择使用哪种路径数据？
**A**:
- 快速预览 → 使用 `routePoints`（点数少）
- 详细查看 → 使用 `trackPoints` + 简化

### Q3: 如何同时显示多条历史路径？
**A**:
```kotlin
histories.forEach { history ->
    val points = repository.parseLatLngListFromJson(history.routePoints)
    drawRoute(points, randomColor())  // 每条路径不同颜色
}
```

### Q4: 路径简化会影响精度吗？
**A**: 使用Douglas-Peucker算法，容差20米时几乎无视觉差异，但数据量可减少80-90%

---

## 📞 需要帮助？

如有问题，请查看：
- [完整API文档](NAVIGATION_HISTORY_API.md)
- [代码示例](NavigationHistoryUsageExample.kt)
- [HistoryMapActivity源码](android-app/app/src/main/java/com/ecogo/app/ui/history/HistoryMapActivity.kt)
