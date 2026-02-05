/**
 * 🚀 快速开始 - 在地图上显示历史路径
 *
 * 这个文件包含最简单的代码示例，可以直接复制到你的项目中使用
 */

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ecogo.app.data.repository.NavigationHistoryRepository
import com.ecogo.app.util.RouteSimplifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.launch
import android.graphics.Color

// ============================================================
// 【方案 1】最简单 - 只需要3步！（推荐新手）
// ============================================================

/**
 * 第一步：获取历史记录
 */
fun step1_GetHistory() {
    lifecycleScope.launch {
        val repo = NavigationHistoryRepository.getInstance()

        // 获取最新的一条记录
        val history = repo.getRecentHistories(1).firstOrNull()

        history?.let {
            // 解析路径点（使用规划路线，数据量小）
            val points = repo.parseLatLngListFromJson(it.routePoints)

            // 第二步：在地图上显示
            step2_DrawOnMap(points)
        }
    }
}

/**
 * 第二步：在地图上绘制
 */
fun step2_DrawOnMap(points: List<LatLng>) {
    // 绘制路线
    googleMap?.addPolyline(
        PolylineOptions()
            .addAll(points)
            .width(10f)
            .color(Color.BLUE)
    )

    // 添加标记
    googleMap?.addMarker(MarkerOptions().position(points.first()).title("起点"))
    googleMap?.addMarker(MarkerOptions().position(points.last()).title("终点"))

    // 第三步：调整视角
    step3_FitCamera(points)
}

/**
 * 第三步：调整相机显示完整路线
 */
fun step3_FitCamera(points: List<LatLng>) {
    val boundsBuilder = LatLngBounds.Builder()
    points.forEach { boundsBuilder.include(it) }

    googleMap?.animateCamera(
        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
    )
}

// ============================================================
// 【方案 2】完整示例 - 一个文件搞定所有
// ============================================================

class SimpleHistoryMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // 初始化地图
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // 🎯 核心代码：加载并显示历史路径
        loadHistoryRoute()
    }

    /**
     * 🔥 核心方法：加载并显示历史路径
     */
    private fun loadHistoryRoute() {
        lifecycleScope.launch {
            val repo = NavigationHistoryRepository.getInstance()

            // 1️⃣ 获取历史记录（这里获取最新的一条）
            val history = repo.getRecentHistories(1).firstOrNull() ?: return@launch

            // 2️⃣ 解析路径点
            val points = repo.parseLatLngListFromJson(history.routePoints)

            // 3️⃣ 在地图上绘制
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(10f)
                    .color(Color.BLUE)
                    .geodesic(true)
            )

            // 4️⃣ 添加起点和终点标记
            googleMap?.addMarker(
                MarkerOptions()
                    .position(points.first())
                    .title(history.originName)
            )

            googleMap?.addMarker(
                MarkerOptions()
                    .position(points.last())
                    .title(history.destinationName)
            )

            // 5️⃣ 调整相机显示完整路线
            val boundsBuilder = LatLngBounds.Builder()
            points.forEach { boundsBuilder.include(it) }
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
            )
        }
    }
}

// ============================================================
// 【方案 3】数据量大时 - 使用路径简化
// ============================================================

fun loadHistoryRouteWithSimplification() {
    lifecycleScope.launch {
        val repo = NavigationHistoryRepository.getInstance()
        val history = repo.getRecentHistories(1).firstOrNull() ?: return@launch

        // 解析实际轨迹（可能有几千个点）
        val trackPoints = repo.parseLatLngListFromJson(history.trackPoints)

        // 🔥 关键：简化路径，减少90%的点数
        val simplifiedPoints = RouteSimplifier.simplify(trackPoints, tolerance = 20.0)

        // 打印对比
        println("原始: ${trackPoints.size} 个点")
        println("简化后: ${simplifiedPoints.size} 个点")

        // 绘制简化后的路径
        googleMap?.addPolyline(
            PolylineOptions()
                .addAll(simplifiedPoints)
                .width(10f)
                .color(Color.GREEN)
        )
    }
}

// ============================================================
// 【方案 4】显示多条历史路径
// ============================================================

fun loadMultipleHistoryRoutes() {
    lifecycleScope.launch {
        val repo = NavigationHistoryRepository.getInstance()

        // 获取最近5条记录
        val histories = repo.getRecentHistories(5)

        // 定义不同的颜色
        val colors = listOf(
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.MAGENTA
        )

        // 遍历每条记录
        histories.forEachIndexed { index, history ->
            val points = repo.parseLatLngListFromJson(history.routePoints)

            // 用不同颜色绘制每条路径
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(8f)
                    .color(colors[index % colors.size])
            )
        }
    }
}

// ============================================================
// 【方案 5】根据ID显示指定的历史路径
// ============================================================

fun loadHistoryRouteById(historyId: Long) {
    lifecycleScope.launch {
        val repo = NavigationHistoryRepository.getInstance()

        // 根据ID获取记录
        val history = repo.getHistoryById(historyId) ?: return@launch

        // 解析并显示
        val points = repo.parseLatLngListFromJson(history.routePoints)

        googleMap?.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(10f)
                .color(Color.BLUE)
        )

        // 显示详细信息
        println("""
            路线: ${history.originName} → ${history.destinationName}
            距离: ${String.format("%.2f", history.totalDistance / 1000)} km
            减碳: ${String.format("%.2f", history.carbonSaved)} kg
            交通方式: ${history.transportMode}
        """.trimIndent())
    }
}

// ============================================================
// 【方案 6】显示今天的所有路径
// ============================================================

fun loadTodayAllRoutes() {
    lifecycleScope.launch {
        val repo = NavigationHistoryRepository.getInstance()

        // 获取今天的所有记录
        val todayHistories = repo.getTodayHistories()

        println("今天共有 ${todayHistories.size} 条出行记录")

        todayHistories.forEach { history ->
            val points = repo.parseLatLngListFromJson(history.routePoints)

            // 绘制每条路径
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(8f)
                    .color(if (history.isGreenTrip) Color.GREEN else Color.GRAY)
            )
        }
    }
}

// ============================================================
// 【方案 7】只显示绿色出行的路径
// ============================================================

fun loadGreenTripsOnly() {
    lifecycleScope.launch {
        val repo = NavigationHistoryRepository.getInstance()

        // 只获取绿色出行记录
        val greenTrips = repo.getGreenTrips()

        println("共有 ${greenTrips.size} 次绿色出行")

        greenTrips.forEach { history ->
            val points = repo.parseLatLngListFromJson(history.routePoints)

            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(10f)
                    .color(Color.parseColor("#4CAF50"))  // 绿色
            )
        }
    }
}

// ============================================================
// 【方案 8】对比规划路线和实际轨迹
// ============================================================

fun comparePlannedAndActual() {
    lifecycleScope.launch {
        val repo = NavigationHistoryRepository.getInstance()
        val history = repo.getRecentHistories(1).firstOrNull() ?: return@launch

        // 规划路线（蓝色虚线）
        val plannedPoints = repo.parseLatLngListFromJson(history.routePoints)
        googleMap?.addPolyline(
            PolylineOptions()
                .addAll(plannedPoints)
                .width(8f)
                .color(Color.BLUE)
                .pattern(listOf(Dash(20f), Gap(10f)))  // 虚线
        )

        // 实际轨迹（绿色实线，简化后）
        val trackPoints = repo.parseLatLngListFromJson(history.trackPoints)
        val simplifiedTrack = RouteSimplifier.simplify(trackPoints, 20.0)
        googleMap?.addPolyline(
            PolylineOptions()
                .addAll(simplifiedTrack)
                .width(10f)
                .color(Color.GREEN)  // 实线
        )
    }
}

// ============================================================
// 💡 使用建议
// ============================================================

/*
📊 根据你的需求选择：

1️⃣ 只是简单显示一条路径？
   → 使用【方案 1】或【方案 2】

2️⃣ 数据量太大，担心性能？
   → 使用【方案 3】的路径简化

3️⃣ 需要显示多条路径？
   → 使用【方案 4】或【方案 6】

4️⃣ 从列表点击进入查看详情？
   → 使用【方案 5】

5️⃣ 只关心绿色出行？
   → 使用【方案 7】

6️⃣ 想看规划和实际的对比？
   → 使用【方案 8】

⚡ 性能优化：
- routePoints：50-200个点，快速加载 ✅ 推荐
- trackPoints：可能上千个点，需要简化
- 简化后：减少90%点数，几乎无视觉差异

🎨 视觉建议：
- 规划路线：蓝色虚线 #4285F4
- 实际轨迹：绿色实线 #4CAF50
- 起点：绿色标记
- 终点：红色标记
*/

// ============================================================
// 🔧 工具函数（可选）
// ============================================================

/**
 * 通用的绘制路径方法
 */
fun drawRoute(
    points: List<LatLng>,
    color: Int = Color.BLUE,
    width: Float = 10f,
    isDashed: Boolean = false
) {
    val options = PolylineOptions()
        .addAll(points)
        .width(width)
        .color(color)
        .geodesic(true)

    if (isDashed) {
        options.pattern(listOf(Dash(20f), Gap(10f)))
    }

    googleMap?.addPolyline(options)
}

/**
 * 添加标记的快捷方法
 */
fun addMarker(position: LatLng, title: String, color: Float = BitmapDescriptorFactory.HUE_RED) {
    googleMap?.addMarker(
        MarkerOptions()
            .position(position)
            .title(title)
            .icon(BitmapDescriptorFactory.defaultMarker(color))
    )
}

// ============================================================
// 📝 完整的复制粘贴模板
// ============================================================

/**
 * 🎯 模板：完整的显示历史路径Activity
 * 直接复制到你的项目中使用！
 */
class ShowHistoryRouteTemplate : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val repo = NavigationHistoryRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(你的布局)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // 🔥 在这里调用你需要的功能
        showLatestRoute()  // 显示最新路径
    }

    /**
     * 显示最新的历史路径
     */
    private fun showLatestRoute() {
        lifecycleScope.launch {
            // 1. 获取数据
            val history = repo.getRecentHistories(1).firstOrNull() ?: return@launch

            // 2. 解析路径点
            val points = repo.parseLatLngListFromJson(history.routePoints)

            // 3. 绘制路线
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(10f)
                    .color(Color.BLUE)
            )

            // 4. 添加标记
            addMarkers(points.first(), points.last(), history)

            // 5. 调整视角
            fitCamera(points)
        }
    }

    private fun addMarkers(start: LatLng, end: LatLng, history: NavigationHistory) {
        googleMap?.addMarker(
            MarkerOptions().position(start).title(history.originName)
        )
        googleMap?.addMarker(
            MarkerOptions().position(end).title(history.destinationName)
        )
    }

    private fun fitCamera(points: List<LatLng>) {
        val builder = LatLngBounds.Builder()
        points.forEach { builder.include(it) }
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngBounds(builder.build(), 100)
        )
    }
}
