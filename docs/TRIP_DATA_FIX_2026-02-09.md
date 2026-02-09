# Trip Data Fix - 2026-02-09

## 问题描述

### 症状
- API 接口：`GET /api/v1/web/trips/all`
- 错误码：5002 (Internal server error)
- 错误信息：`"Internal server error, please try again later"`

### 根本原因
MongoDB 数据库中部分 Trip 记录的 `transport_modes` 字段存储格式错误：
- **错误格式**：JSON 字符串数组 `["{"mode":"walk",...}", ...]`
- **正确格式**：对象数组 `[{mode:"walk",...}, ...]`

### 错误堆栈
```
org.springframework.core.convert.ConverterNotFoundException:
No converter found capable of converting from type [java.lang.String]
to type [com.example.EcoGo.model.Trip$TransportSegment]
```

## 解决方案

### 数据修复脚本
在 MongoDB 中执行以下脚本修复损坏的数据：

```javascript
var count = 0;
db.trips.find({transport_modes: {$type: "string"}}).forEach(function(doc) {
  if (doc.transport_modes && Array.isArray(doc.transport_modes)) {
    var fixed = doc.transport_modes.map(function(item) {
      if (typeof item === "string") {
        try {
          return JSON.parse(item);
        } catch(e) {
          return item;
        }
      }
      return item;
    });
    db.trips.updateOne({_id: doc._id}, {$set: {transport_modes: fixed}});
    count++;
  }
});
print("Fixed " + count + " trip records");
```

### 修复结果
- 修复了 **2 条** 损坏的 Trip 记录
- 所有 Trip 记录的 `transport_modes` 字段现在格式统一

## 预防措施

### 建议
1. **数据验证**：在 `TripServiceImpl.completeTrip()` 方法中添加数据格式验证
2. **单元测试**：添加测试用例验证 `transport_modes` 的序列化/反序列化
3. **数据库约束**：考虑使用 MongoDB Schema Validation 确保数据格式正确

### 监控
定期检查是否有新的格式错误数据：
```javascript
db.trips.find({transport_modes: {$type: "string"}}).count()
```

## 影响范围
- **影响接口**：`GET /api/v1/web/trips/all`
- **影响用户**：Admin 用户无法查看所有行程数据
- **修复时间**：2026-02-09
- **修复人员**：Claude Code Assistant

## 相关文件
- `src/main/java/com/example/EcoGo/model/Trip.java` (第 144-183 行：TransportSegment 定义)
- `src/main/java/com/example/EcoGo/service/TripServiceImpl.java` (第 178-182 行：getAllTrips 方法)
- `src/main/java/com/example/EcoGo/controller/TripController.java` (第 105-108 行：getAllTrips 接口)

## 测试验证
修复后，`GET /api/v1/web/trips/all` 接口应正常返回所有行程数据：
```bash
curl -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  http://47.129.124.55:8090/api/v1/web/trips/all
```

预期响应：
```json
{
  "code": 200,
  "message": "Operation successful",
  "data": [...]
}
```
