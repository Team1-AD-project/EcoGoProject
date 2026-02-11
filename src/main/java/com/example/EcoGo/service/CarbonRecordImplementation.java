package com.example.EcoGo.service;

import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.CarbonRecordInterface;
// import com.example.EcoGo.interfacemethods.UserInterface; // Removed dependency
import com.example.EcoGo.model.CarbonRecord;
import com.example.EcoGo.repository.CarbonRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CarbonRecordImplementation implements CarbonRecordInterface {

    private static final String FIELD_TOTAL_CARBON_SAVED = "totalCarbonSaved";

    @Autowired
    private CarbonRecordRepository carbonRecordRepository;

    // @Autowired
    // private UserInterface userService; // Removed dependency

    @Override
    public List<CarbonRecord> getAllRecords() {
        return carbonRecordRepository.findAll();
    }

    @Override
    public List<CarbonRecord> getRecordsByUserId(String userId) {
        return carbonRecordRepository.findByUserId(userId);
    }

    @Override
    public CarbonRecord createRecord(CarbonRecord record) {
        return carbonRecordRepository.save(record);
    }

    @Override
    public CarbonRecord earnCredits(String userId, Integer credits, String source, String description) {
        // 创建积分记录
        CarbonRecord record = new CarbonRecord(userId, "EARN", credits, source, description);
        CarbonRecord saved = carbonRecordRepository.save(record);

        // 更新用户总积分 (Temporarily disabled)
        // userService.addCarbonCredits(userId, credits);

        return saved;
    }

    @Override
    public CarbonRecord spendCredits(String userId, Integer credits, String source, String description) {
        // 检查用户积分是否足够
        Integer totalCredits = getTotalCreditsByUserId(userId);
        if (totalCredits < credits) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 创建消耗记录
        CarbonRecord record = new CarbonRecord(userId, "SPEND", credits, source, description);
        CarbonRecord saved = carbonRecordRepository.save(record);

        // 扣减用户总积分 (Temporarily disabled)
        // userService.addCarbonCredits(userId, -credits);

        return saved;
    }

    @Override
    public Integer getTotalCreditsByUserId(String userId) {
        List<CarbonRecord> records = carbonRecordRepository.findByUserId(userId);
        int total = 0;
        for (CarbonRecord record : records) {
            if ("EARN".equals(record.getType())) {
                total += record.getCredits();
            } else if ("SPEND".equals(record.getType())) {
                total -= record.getCredits();
            }
        }
        return total;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.EcoGo.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Override
    public com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse getFacultyTotalCarbon(String userId) {
        // 1. Get current user
        com.example.EcoGo.model.User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String faculty = user.getFaculty();
        if (faculty == null || faculty.isEmpty()) {
            return new com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse("", 0.0);
        }

        // 2. Find all users in the same faculty
        List<com.example.EcoGo.model.User> facultyUsers = userRepository.findByFaculty(faculty);
        if (facultyUsers.isEmpty()) {
            return new com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse(faculty, 0.0);
        }

        // 3. Get all User IDs
        List<String> userIds = facultyUsers.stream()
                .map(com.example.EcoGo.model.User::getUserid)
                .collect(java.util.stream.Collectors.toList());

        // 4. Use MongoTemplate aggregation (same approach as LeaderboardImplementation)
        org.springframework.data.mongodb.core.aggregation.Aggregation aggregation =
                org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                        org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                                org.springframework.data.mongodb.core.query.Criteria
                                        .where("carbon_status").is("completed")
                                        .and("user_id").in(userIds)),
                        org.springframework.data.mongodb.core.aggregation.Aggregation
                                .group().sum("carbon_saved").as(FIELD_TOTAL_CARBON_SAVED)
                );

        org.springframework.data.mongodb.core.aggregation.AggregationResults<java.util.Map> results =
                mongoTemplate.aggregate(aggregation, "trips", java.util.Map.class);

        double totalCarbon = 0.0;
        java.util.Map result = results.getUniqueMappedResult();
        if (result != null && result.get(FIELD_TOTAL_CARBON_SAVED) != null) {
            totalCarbon = ((Number) result.get(FIELD_TOTAL_CARBON_SAVED)).doubleValue();
        }

        // 5. Round to 2 decimal places
        totalCarbon = Math.round(totalCarbon * 100.0) / 100.0;

        return new com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse(faculty, totalCarbon);
    }
}
