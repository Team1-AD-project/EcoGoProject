# EcoGo Trip API 请求与响应模板

Base URL: `http://47.129.124.55:8090/api/v1`

所有API返回统一的 `ResponseMessage<T>` 格式：
```json
{
  "code": 200,
  "message": "success!",
  "data": { /* 实际数据 */ }
}
```

---

## 1. 开始行程

**POST** `/mobile/trips/start`

### 请求 Header:
```
Authorization: Bearer <token>
Content-Type: application/json
```

### 请求 Body:
```json
{
  "startLng": 114.179900,
  "startLat": 22.337400,
  "startAddress": "广东省深圳市南山区南海大道3688号",
  "startPlaceName": "深圳大学",
  "startCampusZone": "南校区"
}
```

**字段说明：**
| 字段名 | 类型 | 必需 | 说明 | 示例 |
|--------|------|------|------|------|
| startLng | number | 是 | 起点经度 | 114.179900 |
| startLat | number | 是 | 起点纬度 | 22.337400 |
| startAddress | string | 是 | 起点完整地址 | "广东省深圳市南山区南海大道3688号" |
| startPlaceName | string | 是 | 起点地点名称 | "深圳大学" |
| startCampusZone | string | 否 | 起点校区/区域 | "南校区" |

### 返回响应 (HTTP 200):
```json
{
  "code": 200,
  "message": "success!",
  "data": {
    "id": "59f16f60-83bb-4422-9045-626f29b4dc32",
    "userId": "e1538488",
    "startPoint": {
      "lng": 114.179900,
      "lat": 22.337400
    },
    "endPoint": null,
    "startLocation": {
      "address": "广东省深圳市南山区南海大道3688号",
      "placeName": "深圳大学",
      "campusZone": "南校区"
    },
    "endLocation": null,
    "startTime": "2024-02-15T14:30:00",
    "endTime": null,
    "transportModes": null,
    "detectedMode": null,
    "mlConfidence": 0.0,
    "isGreenTrip": false,
    "distance": 0.0,
    "polylinePoints": null,
    "carbonSaved": 0,
    "pointsGained": 0,
    "carbonStatus": "tracking",
    "createdAt": "2024-02-15T14:30:00"
  }
}
```

**响应字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | string | 行程唯一ID（完成行程时用这个） |
| userId | string | 用户ID |
| startPoint | object | 起点坐标（嵌套对象） |
| startPoint.lng | number | 起点经度 |
| startPoint.lat | number | 起点纬度 |
| endPoint | object/null | 终点坐标（开始时为null） |
| startLocation | object | 起点位置详情（嵌套对象） |
| startLocation.address | string | 起点完整地址 |
| startLocation.placeName | string | 起点地点名称 |
| startLocation.campusZone | string | 起点校区 |
| endLocation | object/null | 终点位置详情（开始时为null） |
| startTime | string | 开始时间（ISO 8601） |
| endTime | string/null | 结束时间（开始时为null） |
| transportModes | array/null | 交通方式列表（开始时为null） |
| detectedMode | string/null | 检测的交通方式（开始时为null） |
| mlConfidence | number(double) | ML置信度 |
| isGreenTrip | boolean | 是否绿色出行 |
| distance | number(double) | 距离，单位：公里 |
| polylinePoints | array/null | 轨迹点列表（开始时为null） |
| carbonSaved | long | 减碳量，单位：克(g) |
| pointsGained | long | 获得积分 |
| carbonStatus | string | 行程状态："tracking"（追踪中） |
| createdAt | string | 创建时间（ISO 8601） |

### Kotlin 示例:
```kotlin
val repo = TripRepository.getInstance()
repo.setAuthToken("your_jwt_token")

val result = repo.startTrip(
    startLat = 22.337400,
    startLng = 114.179900,
    startPlaceName = "深圳大学",
    startAddress = "广东省深圳市南山区南海大道3688号",
    startCampusZone = "南校区"
)

result.onSuccess { tripId ->
    Log.d(TAG, "Trip started: $tripId")
    // 保存这个 ID，完成行程时要用
}
result.onFailure { error ->
    Log.e(TAG, "Failed to start trip: ${error.message}")
}
```

---

## 2. 完成行程

**POST** `/mobile/trips/{tripId}/complete`

### 请求 Header:
```
Authorization: Bearer <token>
Content-Type: application/json
```

### Path 参数:
- `tripId`: 从"开始行程"API 返回的 `data.id` 字段值

### 请求 Body:
```json
{
  "endLng": 114.183456,
  "endLat": 22.345678,
  "endAddress": "广东省深圳市南山区科技园南区",
  "endPlaceName": "科技园地铁站",
  "endCampusZone": "高新区",
  "distance": 2.5,
  "detectedMode": "walk",
  "mlConfidence": 0.92,
  "isGreenTrip": true,
  "carbonSaved": 850,
  "transportModes": [
    {
      "mode": "walk",
      "subDistance": 0.8,
      "subDuration": 12
    },
    {
      "mode": "subway",
      "subDistance": 1.5,
      "subDuration": 8
    },
    {
      "mode": "walk",
      "subDistance": 0.2,
      "subDuration": 3
    }
  ],
  "polylinePoints": [
    {"lng": 114.179900, "lat": 22.337400},
    {"lng": 114.180500, "lat": 22.338200},
    {"lng": 114.181200, "lat": 22.339500},
    {"lng": 114.182000, "lat": 22.341000},
    {"lng": 114.183456, "lat": 22.345678}
  ]
}
```

**字段说明：**
| 字段名 | 类型 | 必需 | 说明 | 示例 |
|--------|------|------|------|------|
| endLng | number | 是 | 终点经度 | 114.183456 |
| endLat | number | 是 | 终点纬度 | 22.345678 |
| endAddress | string | 是 | 终点完整地址 | "广东省深圳市南山区科技园南区" |
| endPlaceName | string | 是 | 终点地点名称 | "科技园地铁站" |
| endCampusZone | string | 否 | 终点校区/区域 | "高新区" |
| distance | number(double) | 是 | 总距离，单位：公里 | 2.5 |
| detectedMode | string | 否 | 主要交通方式 | "walk"（见下方有效值） |
| mlConfidence | number(double) | 否 | ML模型置信度 (0-1) | 0.92 |
| isGreenTrip | boolean | 是 | 是否绿色出行 | true |
| carbonSaved | long | 是 | 减碳量，单位：克(g) | 850 |
| transportModes | array | 否 | 交通方式分段列表 | 见上方示例 |
| polylinePoints | array | 否 | 轨迹点坐标列表 | 见上方示例 |

**有效的交通方式值（detectedMode / mode）：**
- `"walk"` - 步行
- `"bike"` - 自行车
- `"bus"` - 公交
- `"subway"` - 地铁
- `"car"` - 私家车
- `"electric_bike"` - 电动车

**transportModes 数组元素：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| mode | string | 交通方式（使用上方有效值） |
| subDistance | number(double) | 该段距离，单位：公里 |
| subDuration | int | 该段时长，单位：分钟 |

**polylinePoints 数组元素：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| lng | number(double) | 经度 |
| lat | number(double) | 纬度 |

### 返回响应 (HTTP 200):
```json
{
  "code": 200,
  "message": "success!",
  "data": {
    "id": "59f16f60-83bb-4422-9045-626f29b4dc32",
    "userId": "e1538488",
    "startPoint": {
      "lng": 114.179900,
      "lat": 22.337400
    },
    "endPoint": {
      "lng": 114.183456,
      "lat": 22.345678
    },
    "startLocation": {
      "address": "广东省深圳市南山区南海大道3688号",
      "placeName": "深圳大学",
      "campusZone": "南校区"
    },
    "endLocation": {
      "address": "广东省深圳市南山区科技园南区",
      "placeName": "科技园地铁站",
      "campusZone": "高新区"
    },
    "startTime": "2024-02-15T14:30:00",
    "endTime": "2024-02-15T15:00:00",
    "transportModes": [
      {
        "mode": "walk",
        "subDistance": 0.8,
        "subDuration": 12
      },
      {
        "mode": "subway",
        "subDistance": 1.5,
        "subDuration": 8
      },
      {
        "mode": "walk",
        "subDistance": 0.2,
        "subDuration": 3
      }
    ],
    "detectedMode": "walk",
    "mlConfidence": 0.92,
    "isGreenTrip": true,
    "distance": 2.5,
    "polylinePoints": [
      {"lng": 114.179900, "lat": 22.337400},
      {"lng": 114.180500, "lat": 22.338200},
      {"lng": 114.181200, "lat": 22.339500},
      {"lng": 114.182000, "lat": 22.341000},
      {"lng": 114.183456, "lat": 22.345678}
    ],
    "carbonSaved": 850,
    "pointsGained": 85,
    "carbonStatus": "completed",
    "createdAt": "2024-02-15T14:30:00"
  }
}
```

**响应字段说明：**
（字段结构同"开始行程"响应，但已填充完整数据）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | string | 行程唯一ID |
| userId | string | 用户ID |
| startPoint | object | 起点坐标 |
| endPoint | object | 终点坐标（已填充） |
| startLocation | object | 起点位置详情 |
| endLocation | object | 终点位置详情（已填充） |
| startTime | string | 开始时间 |
| endTime | string | 结束时间（已填充） |
| transportModes | array | 交通方式分段列表（已填充） |
| detectedMode | string | 检测的交通方式 |
| mlConfidence | number(double) | ML置信度 |
| isGreenTrip | boolean | 是否绿色出行 |
| distance | number(double) | 距离，单位：公里 |
| polylinePoints | array | 轨迹点列表（已填充） |
| carbonSaved | long | 减碳量，单位：克(g) |
| pointsGained | long | 获得积分（后端根据交通方式和距离自动计算） |
| carbonStatus | string | 行程状态："completed"（已完成） |
| createdAt | string | 创建时间 |

### Kotlin 示例:
```kotlin
val trackPoints = listOf(
    LatLng(22.337400, 114.179900),
    LatLng(22.338200, 114.180500),
    LatLng(22.339500, 114.181200),
    LatLng(22.341000, 114.182000),
    LatLng(22.345678, 114.183456)
)

val result = repo.completeTrip(
    tripId = "59f16f60-83bb-4422-9045-626f29b4dc32", // 从开始行程获取的 ID
    endLat = 22.345678,
    endLng = 114.183456,
    endPlaceName = "科技园地铁站",
    endAddress = "广东省深圳市南山区科技园南区",
    distance = 2500.0,  // 单位：米（TripRepository内部会转为公里）
    trackPoints = trackPoints,
    transportMode = "walk",  // ⚠️ 必须使用小写有效值: walk/bike/bus/subway/car/electric_bike
    detectedMode = "walk",
    mlConfidence = 0.92,
    carbonSaved = 850L,  // 单位：克(g)
    isGreenTrip = true
)

result.onSuccess { tripResponse ->
    Log.d(TAG, "Trip completed!")
    Log.d(TAG, "Carbon saved: ${tripResponse.carbonSaved}g")
    Log.d(TAG, "Points gained: ${tripResponse.pointsGained}")
}
```

---

## 3. 取消行程

**POST** `/mobile/trips/{tripId}/cancel`

### 请求 Header:
```
Authorization: Bearer <token>
```

### Path 参数:
- `tripId`: 要取消的行程ID

### 请求 Body:
```
无请求体
```

### 返回响应 (HTTP 200):
```json
{
  "code": 200,
  "message": "success!",
  "data": "Trip canceled"
}
```

**响应字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | integer | 状态码：200表示成功 |
| message | string | 响应消息 |
| data | string | 取消结果消息 |

### Kotlin 示例:
```kotlin
val result = repo.cancelTrip(tripId = "59f16f60-83bb-4422-9045-626f29b4dc32")

result.onSuccess { message ->
    Log.d(TAG, "Trip canceled: $message")
}
result.onFailure { error ->
    Log.e(TAG, "Failed to cancel trip: ${error.message}")
}
```

---

## 4. 获取行程列表

**GET** `/mobile/trips`

### 请求 Header:
```
Authorization: Bearer <token>
```

### Query 参数:
**后端当前不支持任何查询参数。** 直接返回该用户的全部行程列表（按创建时间倒序排列）。

### 示例URL:
```
GET /mobile/trips
```

### 返回响应 (HTTP 200):
```json
{
  "code": 200,
  "message": "success!",
  "data": [
    {
      "id": "59f16f60-83bb-4422-9045-626f29b4dc32",
      "startPlaceName": "深圳大学",
      "endPlaceName": "科技园地铁站",
      "detectedMode": "walk",
      "distance": 2.5,
      "carbonSaved": 850,
      "pointsGained": 85,
      "isGreenTrip": true,
      "carbonStatus": "completed",
      "startTime": "2024-02-15T14:30:00",
      "endTime": "2024-02-15T15:00:00"
    },
    {
      "id": "abc123-def456-ghi789",
      "startPlaceName": "科兴科学园",
      "endPlaceName": "深圳湾公园",
      "detectedMode": "bike",
      "distance": 1.8,
      "carbonSaved": 600,
      "pointsGained": 60,
      "isGreenTrip": true,
      "carbonStatus": "completed",
      "startTime": "2024-02-14T09:00:00",
      "endTime": "2024-02-14T09:30:00"
    }
  ]
}
```

**响应字段说明（TripSummaryResponse - 简化版）：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | string | 行程唯一ID |
| startPlaceName | string | 起点地点名称（扁平字段，非嵌套对象） |
| endPlaceName | string | 终点地点名称（扁平字段，非嵌套对象） |
| detectedMode | string | 检测的交通方式 |
| distance | number(double) | 距离，单位：公里 |
| carbonSaved | long | 减碳量，单位：克(g) |
| pointsGained | long | 获得积分 |
| isGreenTrip | boolean | 是否绿色出行 |
| carbonStatus | string | 行程状态 |
| startTime | string | 开始时间（ISO 8601） |
| endTime | string | 结束时间（ISO 8601） |

**注意：**
- 列表API返回简化的 **TripSummaryResponse**，不包含嵌套对象（startPoint, endPoint, startLocation, endLocation）
- 只包含地点名称字符串（startPlaceName, endPlaceName）
- 不包含详细的坐标、地址、轨迹点等信息
- 如需完整信息，请使用 "获取行程详情" API
- **后端不支持分页和状态筛选**，返回用户全部行程（按创建时间倒序）

### Kotlin 示例:
```kotlin
// 获取所有行程（简化版列表）
val result = repo.getTripListFromCloud()

result.onSuccess { trips ->
    trips.forEach { trip ->
        // 注意：列表返回的是 TripSummaryResponse，只有地点名称字符串
        Log.d(TAG, "Trip: ${trip.startPlaceName} -> ${trip.endPlaceName}")
        Log.d(TAG, "Distance: ${trip.distance}km")
        Log.d(TAG, "Carbon saved: ${trip.carbonSaved}g")
        Log.d(TAG, "Points: ${trip.pointsGained}")
        Log.d(TAG, "Status: ${trip.carbonStatus}")
    }
}
```

---

## 5. 获取行程详情

**GET** `/mobile/trips/{tripId}`

### 请求 Header:
```
Authorization: Bearer <token>
```

### Path 参数:
- `tripId`: 行程ID

### 示例URL:
```
GET /mobile/trips/59f16f60-83bb-4422-9045-626f29b4dc32
```

### 返回响应 (HTTP 200):
```json
{
  "code": 200,
  "message": "success!",
  "data": {
    "id": "59f16f60-83bb-4422-9045-626f29b4dc32",
    "userId": "e1538488",
    "startPoint": {
      "lng": 114.179900,
      "lat": 22.337400
    },
    "endPoint": {
      "lng": 114.183456,
      "lat": 22.345678
    },
    "startLocation": {
      "address": "广东省深圳市南山区南海大道3688号",
      "placeName": "深圳大学",
      "campusZone": "南校区"
    },
    "endLocation": {
      "address": "广东省深圳市南山区科技园南区",
      "placeName": "科技园地铁站",
      "campusZone": "高新区"
    },
    "startTime": "2024-02-15T14:30:00",
    "endTime": "2024-02-15T15:00:00",
    "transportModes": [
      {
        "mode": "walk",
        "subDistance": 0.8,
        "subDuration": 12
      },
      {
        "mode": "subway",
        "subDistance": 1.5,
        "subDuration": 8
      },
      {
        "mode": "walk",
        "subDistance": 0.2,
        "subDuration": 3
      }
    ],
    "detectedMode": "walk",
    "mlConfidence": 0.92,
    "isGreenTrip": true,
    "distance": 2.5,
    "polylinePoints": [
      {"lng": 114.179900, "lat": 22.337400},
      {"lng": 114.180500, "lat": 22.338200},
      {"lng": 114.181200, "lat": 22.339500},
      {"lng": 114.182000, "lat": 22.341000},
      {"lng": 114.183456, "lat": 22.345678}
    ],
    "carbonSaved": 850,
    "pointsGained": 85,
    "carbonStatus": "completed",
    "createdAt": "2024-02-15T14:30:00"
  }
}
```

**响应字段说明：**
- `code`: 状态码，200表示成功
- `message`: 响应消息
- `data`: 行程详细信息（字段同"完成行程"响应）

### Kotlin 示例:
```kotlin
val result = repo.getTripDetail(tripId = "59f16f60-83bb-4422-9045-626f29b4dc32")

result.onSuccess { trip ->
    Log.d(TAG, "Trip ID: ${trip.id}")
    Log.d(TAG, "From: ${trip.startLocation.placeName}")
    Log.d(TAG, "To: ${trip.endLocation?.placeName}")
    Log.d(TAG, "Distance: ${trip.distance}km")
    Log.d(TAG, "Mode: ${trip.detectedMode}")
    Log.d(TAG, "Carbon saved: ${trip.carbonSaved}g")
    Log.d(TAG, "Points: ${trip.pointsGained}")
    Log.d(TAG, "Status: ${trip.carbonStatus}")
}
```

---

## 6. 获取当前追踪行程

**GET** `/mobile/trips/current`

### 请求 Header:
```
Authorization: Bearer <token>
```

### 返回响应 (HTTP 200) - 有当前行程:
```json
{
  "code": 200,
  "message": "success!",
  "data": {
    "id": "59f16f60-83bb-4422-9045-626f29b4dc32",
    "userId": "e1538488",
    "startPoint": {
      "lng": 114.179900,
      "lat": 22.337400
    },
    "endPoint": null,
    "startLocation": {
      "address": "广东省深圳市南山区南海大道3688号",
      "placeName": "深圳大学",
      "campusZone": "南校区"
    },
    "endLocation": null,
    "startTime": "2024-02-15T14:30:00",
    "endTime": null,
    "transportModes": null,
    "detectedMode": null,
    "mlConfidence": 0.0,
    "isGreenTrip": false,
    "distance": 0.0,
    "polylinePoints": null,
    "carbonSaved": 0,
    "pointsGained": 0,
    "carbonStatus": "tracking",
    "createdAt": "2024-02-15T14:30:00"
  }
}
```

### 返回响应 (HTTP 200) - 无当前行程:
```json
{
  "code": 200,
  "message": "success!",
  "data": null
}
```

**响应字段说明：**
- `code`: 状态码，200表示成功
- `message`: 响应消息
- `data`: 当前行程信息（如果有），如果没有当前行程则为 `null`

### Kotlin 示例:
```kotlin
val result = repo.getCurrentTrip()

result.onSuccess { trip ->
    if (trip != null) {
        Log.d(TAG, "Current trip found: ${trip.id}")
        Log.d(TAG, "Started at: ${trip.startLocation.placeName}")
        Log.d(TAG, "Start time: ${trip.startTime}")
    } else {
        Log.d(TAG, "No current trip")
    }
}
```

---

## 7. [Admin] 获取所有行程

**GET** `/web/trips/all`

### 请求 Header:
```
Authorization: Bearer <admin_token>
```

### 返回响应 (HTTP 200):
```json
{
  "code": 200,
  "message": "success!",
  "data": [
    {
      "id": "59f16f60-83bb-4422-9045-626f29b4dc32",
      "startPlaceName": "深圳大学",
      "endPlaceName": "科技园地铁站",
      "detectedMode": "walk",
      "distance": 2.5,
      "carbonSaved": 850,
      "pointsGained": 85,
      "isGreenTrip": true,
      "carbonStatus": "completed",
      "startTime": "2024-02-15T14:30:00",
      "endTime": "2024-02-15T15:00:00"
    }
  ]
}
```

**说明：** 返回格式同行程列表（TripSummaryResponse），包含所有用户的行程。

---

## 8. [Admin] 获取指定用户的行程

**GET** `/web/trips/user/{userid}`

### 请求 Header:
```
Authorization: Bearer <admin_token>
```

### Path 参数:
- `userid`: 目标用户ID

### 返回响应 (HTTP 200):
```json
{
  "code": 200,
  "message": "success!",
  "data": [
    {
      "id": "abc123-def456-ghi789",
      "startPlaceName": "科兴科学园",
      "endPlaceName": "深圳湾公园",
      "detectedMode": "bike",
      "distance": 1.8,
      "carbonSaved": 600,
      "pointsGained": 60,
      "isGreenTrip": true,
      "carbonStatus": "completed",
      "startTime": "2024-02-14T09:00:00",
      "endTime": "2024-02-14T09:30:00"
    }
  ]
}
```

**说明：** 返回格式同行程列表（TripSummaryResponse），只包含指定用户的行程。

---

## 错误响应格式

所有API在发生错误时返回统一格式（使用自定义错误码，非标准HTTP状态码）：

### 4001 - 参数错误 (PARAM_ERROR):
```json
{
  "code": 4001,
  "message": "Parameter error: Invalid transport mode: walking",
  "data": null
}
```

### 4002 - 未登录 (NOT_LOGIN):
```json
{
  "code": 4002,
  "message": "Please login first",
  "data": null
}
```

### 4003 - 无权限 (NO_PERMISSION):
```json
{
  "code": 4003,
  "message": "Permission denied",
  "data": null
}
```

### 4005 - 账户已停用 (ACCOUNT_DISABLED):
```json
{
  "code": 4005,
  "message": "Account is deactivated",
  "data": null
}
```

### 4101 - 用户不存在 (USER_NOT_FOUND):
```json
{
  "code": 4101,
  "message": "User not found",
  "data": null
}
```

### 4601 - 行程不存在 (TRIP_NOT_FOUND):
```json
{
  "code": 4601,
  "message": "Trip not found",
  "data": null
}
```

### 4602 - 行程状态错误 (TRIP_STATUS_ERROR):
```json
{
  "code": 4602,
  "message": "Trip status error, current status: completed",
  "data": null
}
```

### 5002 - 服务器内部错误 (SYSTEM_ERROR):
```json
{
  "code": 5002,
  "message": "Internal server error, please try again later",
  "data": null
}
```

**完整错误码对照表：**
| 错误码 | 枚举名 | 说明 |
|--------|--------|------|
| 200 | SUCCESS | 操作成功 |
| 4001 | PARAM_ERROR | 参数错误（支持动态消息） |
| 4002 | NOT_LOGIN | 未登录 |
| 4003 | NO_PERMISSION | 无权限 |
| 4005 | ACCOUNT_DISABLED | 账户已停用 |
| 4101 | USER_NOT_FOUND | 用户不存在 |
| 4601 | TRIP_NOT_FOUND | 行程不存在 |
| 4602 | TRIP_STATUS_ERROR | 行程状态错误（支持动态消息） |
| 5001 | DB_ERROR | 数据库操作失败 |
| 5002 | SYSTEM_ERROR | 服务器内部错误 |

---

## Token 获取

Token需要从登录系统获取。登录成功后会返回JWT token：

```kotlin
// 登录成功后设置token
val token = loginResponse.token
TripRepository.getInstance().setAuthToken(token)

// 之后所有API调用都会自动使用这个token
```

**Token格式：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 交通方式枚举

### detectedMode / mode 的有效值：
| mode值 | 中文名 | carbonFactor (g/km) | isGreen |
|--------|--------|---------------------|---------|
| `"walk"` | 步行 | 0 | true |
| `"bike"` | 自行车 | 0 | true |
| `"bus"` | 公交 | 20 | true |
| `"subway"` | 地铁 | 10 | true |
| `"car"` | 私家车 | 100 | false |
| `"electric_bike"` | 电动车 | 5 | true |

**注意：**
- 不要使用 `"walking"` 或 `"cycling"`
- 使用 `"walk"` 和 `"bike"`
- 这些值存储在后端 `transport_modes_dict` 集合中

### carbonStatus（行程状态）可能的值：
- `"tracking"` - 追踪中
- `"completed"` - 已完成
- `"canceled"` - 已取消（注意：单 l，不是 cancelled）

---

## 使用建议

### 1. 开始行程流程
```kotlin
// 1. 获取当前位置
val currentLocation = getCurrentLocation()

// 2. 反向地理编码获取地址
val address = getAddressFromLocation(currentLocation)

// 3. 调用开始行程API
val result = TripRepository.getInstance().startTrip(
    startLat = currentLocation.latitude,
    startLng = currentLocation.longitude,
    startPlaceName = address.placeName,
    startAddress = address.fullAddress,
    startCampusZone = address.zone
)

// 4. 保存 tripId（实际上是 data.id）用于后续完成行程
result.onSuccess { tripId ->
    saveCurrentTripId(tripId)
}
```

### 2. 追踪路径
```kotlin
// 在行程进行中持续收集GPS点
val trackPoints = mutableListOf<LatLng>()

locationManager.requestLocationUpdates { location ->
    trackPoints.add(LatLng(location.latitude, location.longitude))
}
```

### 3. 完成行程流程
```kotlin
// 1. 简化轨迹点（减少数据量）
val simplifiedPoints = RouteSimplifier.simplify(
    points = trackPoints,
    tolerance = 20.0  // 20米容差
)

Log.d(TAG, "Original points: ${trackPoints.size}")
Log.d(TAG, "Simplified points: ${simplifiedPoints.size}")

// 2. 计算总距离
val totalDistance = calculateTotalDistance(simplifiedPoints)

// 3. 计算碳减排量（根据距离和交通方式）
val carbonSaved = calculateCarbonSaved(totalDistance, transportMode)

// 4. 调用完成行程API
val result = TripRepository.getInstance().completeTrip(
    tripId = savedTripId,
    endLat = currentLocation.latitude,
    endLng = currentLocation.longitude,
    endPlaceName = endAddress.placeName,
    endAddress = endAddress.fullAddress,
    distance = totalDistance,        // 单位：米（TripRepository内部转为公里）
    trackPoints = simplifiedPoints,
    transportMode = "walk",          // ⚠️ 必须使用小写有效值
    detectedMode = "walk",
    carbonSaved = carbonSaved,
    isGreenTrip = isGreenTransport(transportMode)
)
```

---

## cURL 测试示例

### 1. 开始行程:
```bash
curl -X POST http://47.129.124.55:8090/api/v1/mobile/trips/start \
  -H "Authorization: Bearer your_token_here" \
  -H "Content-Type: application/json" \
  -d '{
    "startLng": 114.179900,
    "startLat": 22.337400,
    "startAddress": "广东省深圳市南山区南海大道3688号",
    "startPlaceName": "深圳大学",
    "startCampusZone": "南校区"
  }'
```

### 2. 完成行程:
```bash
curl -X POST http://47.129.124.55:8090/api/v1/mobile/trips/59f16f60-83bb-4422-9045-626f29b4dc32/complete \
  -H "Authorization: Bearer your_token_here" \
  -H "Content-Type: application/json" \
  -d '{
    "endLng": 114.183456,
    "endLat": 22.345678,
    "endAddress": "广东省深圳市南山区科技园南区",
    "endPlaceName": "科技园地铁站",
    "endCampusZone": "高新区",
    "distance": 2.5,
    "detectedMode": "walk",
    "mlConfidence": 0.92,
    "isGreenTrip": true,
    "carbonSaved": 850,
    "transportModes": [
      {
        "mode": "walk",
        "subDistance": 0.8,
        "subDuration": 12
      }
    ],
    "polylinePoints": [
      {"lng": 114.179900, "lat": 22.337400},
      {"lng": 114.183456, "lat": 22.345678}
    ]
  }'
```

### 3. 获取行程列表:
```bash
curl -X GET http://47.129.124.55:8090/api/v1/mobile/trips \
  -H "Authorization: Bearer your_token_here"
```

### 4. 获取行程详情:
```bash
curl -X GET http://47.129.124.55:8090/api/v1/mobile/trips/59f16f60-83bb-4422-9045-626f29b4dc32 \
  -H "Authorization: Bearer your_token_here"
```

### 5. 获取当前行程:
```bash
curl -X GET http://47.129.124.55:8090/api/v1/mobile/trips/current \
  -H "Authorization: Bearer your_token_here"
```

### 6. 取消行程:
```bash
curl -X POST http://47.129.124.55:8090/api/v1/mobile/trips/59f16f60-83bb-4422-9045-626f29b4dc32/cancel \
  -H "Authorization: Bearer your_token_here"
```

### 7. [Admin] 获取所有行程:
```bash
curl -X GET http://47.129.124.55:8090/api/v1/web/trips/all \
  -H "Authorization: Bearer admin_token_here"
```

### 8. [Admin] 获取指定用户行程:
```bash
curl -X GET http://47.129.124.55:8090/api/v1/web/trips/user/e1538488 \
  -H "Authorization: Bearer admin_token_here"
```

---

## 注意事项

1. **所有API都需要Authorization header**
2. **carbonSaved单位是克(g)，不是千克(kg)**
   - 前端显示时需要转换：`carbonSaved / 1000.0` = kg
3. **distance单位是公里(km)**
   - Android TripRepository.completeTrip() 接收米，内部会除以1000转为公里
4. **时间格式使用ISO 8601标准** (`2024-02-15T14:30:00`)
5. **经纬度使用WGS84坐标系**
6. **轨迹点建议简化后再上传（使用RouteSimplifier）**
   - 可以减少90%的数据量，提高传输效率
7. **Token过期后需要重新登录获取新token**
8. **后端返回的是嵌套结构**：
   - 使用 `id` 不是 `tripId`
   - 使用 `startPoint.lng` 不是 `startLng`
   - 使用 `startLocation.address` 不是 `startAddress`
9. **交通方式使用正确的值**：
   - 使用 `"walk"` 不是 `"walking"`
   - 使用 `"bike"` 不是 `"cycling"`
10. **错误码使用自定义码**（非标准HTTP码）：
    - 4001 参数错误，4002 未登录，4003 无权限
    - 4601 行程不存在，4602 行程状态错误
    - 5002 服务器内部错误

---

## 本次修正记录

相比之前的版本，本次修正了以下关键问题：

1. **GET /mobile/trips 移除了不存在的查询参数**
   - 后端不支持 `page`, `pageSize`, `status` 参数
   - 直接返回用户全部行程（按创建时间倒序）

2. **carbonStatus 取消状态拼写修正**
   - `"cancelled"` → `"canceled"`（后端使用单l拼写）

3. **错误响应码全部修正为后端实际值**
   - 400 → 4001 (PARAM_ERROR)
   - 401 → 4002 (NOT_LOGIN)
   - 404 → 4601 (TRIP_NOT_FOUND)
   - 500 → 5002 (SYSTEM_ERROR)
   - 新增 4003 (NO_PERMISSION), 4602 (TRIP_STATUS_ERROR) 等
   - 错误消息修正为后端实际的英文消息

4. **Kotlin示例中 transportMode 修正**
   - `"WALKING"` → `"walk"`（必须使用小写有效值）

5. **新增 Admin API 文档**
   - GET /web/trips/all（获取所有行程）
   - GET /web/trips/user/{userid}（获取指定用户行程）

6. **交通方式表格增加 carbonFactor 和 isGreen 信息**
   - 来源于后端 DatabaseSeeder 的 transport_modes_dict 种子数据

7. **startPoint 示例坐标修正**
   - 开始行程响应中 startPoint 坐标与请求体一致（之前示例中坐标不匹配）

---

**最后更新时间：** 2026-02-06
