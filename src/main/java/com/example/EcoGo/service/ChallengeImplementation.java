package com.example.EcoGo.service;

import com.example.EcoGo.dto.UserChallengeProgressDTO;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.ChallengeInterface;
import com.example.EcoGo.model.Challenge;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserChallengeProgress;
import com.example.EcoGo.repository.ChallengeRepository;
import com.example.EcoGo.repository.UserChallengeProgressRepository;
import com.example.EcoGo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChallengeImplementation implements ChallengeInterface {

    private static final String FIELD_CHALLENGE_ID = "challenge_id";
    private static final String FIELD_USER_ID = "user_id";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String TRIPS_COLLECTION = "trips";
    private static final String AGGREGATION_TOTAL_KEY = "total";

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserChallengeProgressRepository userChallengeProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private com.example.EcoGo.interfacemethods.PointsService pointsService;

    @Autowired
    private com.example.EcoGo.repository.UserPointsLogRepository userPointsLogRepository;

    @Override
    public List<Challenge> getAllChallenges() {
        List<Challenge> challenges = challengeRepository.findAll();
        for (Challenge challenge : challenges) {
            Query countQuery = new Query(Criteria.where(FIELD_CHALLENGE_ID).is(challenge.getId()));
            long participantCount = mongoTemplate.count(countQuery, UserChallengeProgress.class);
            challenge.setParticipants((int) participantCount);
        }
        return challenges;
    }

    @Override
    public Challenge getChallengeById(String id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));
    }

    @Override
    public Challenge createChallenge(Challenge challenge) {
        challenge.setCreatedAt(LocalDateTime.now());
        challenge.setUpdatedAt(LocalDateTime.now());
        if (challenge.getStatus() == null) challenge.setStatus("ACTIVE");
        if (challenge.getParticipants() == null) challenge.setParticipants(0);
        if (challenge.getIcon() == null) challenge.setIcon("\uD83C\uDFC6");
        return challengeRepository.save(challenge);
    }

    @Override
    public Challenge updateChallenge(String id, Challenge challenge) {
        Challenge existing = challengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        if (challenge.getTitle() != null) existing.setTitle(challenge.getTitle());
        if (challenge.getDescription() != null) existing.setDescription(challenge.getDescription());
        if (challenge.getType() != null) existing.setType(challenge.getType());
        if (challenge.getTarget() != null) existing.setTarget(challenge.getTarget());
        if (challenge.getReward() != null) existing.setReward(challenge.getReward());
        if (challenge.getBadge() != null) existing.setBadge(challenge.getBadge());
        if (challenge.getIcon() != null) existing.setIcon(challenge.getIcon());
        if (challenge.getStatus() != null) existing.setStatus(challenge.getStatus());
        if (challenge.getStartTime() != null) existing.setStartTime(challenge.getStartTime());
        if (challenge.getEndTime() != null) existing.setEndTime(challenge.getEndTime());
        existing.setUpdatedAt(LocalDateTime.now());

        return challengeRepository.save(existing);
    }

    @Override
    public void deleteChallenge(String id) {
        if (!challengeRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        Query deleteQuery = new Query(Criteria.where(FIELD_CHALLENGE_ID).is(id));
        mongoTemplate.remove(deleteQuery, UserChallengeProgress.class);
        challengeRepository.deleteById(id);
    }

    @Override
    public List<Challenge> getChallengesByStatus(String status) {
        return challengeRepository.findByStatus(status);
    }

    @Override
    public List<Challenge> getChallengesByType(String type) {
        return challengeRepository.findByType(type);
    }

    @Override
    public List<Challenge> getChallengesByUserId(String userId) {
        Query query = new Query(Criteria.where(FIELD_USER_ID).is(userId));
        List<UserChallengeProgress> userProgress = mongoTemplate.find(query, UserChallengeProgress.class);
        List<String> challengeIds = userProgress.stream()
                .map(UserChallengeProgress::getChallengeId)
                .collect(Collectors.toList());

        if (challengeIds.isEmpty()) {
            return new ArrayList<>();
        }
        return challengeRepository.findAllById(challengeIds);
    }

    @Override
    public UserChallengeProgress joinChallenge(String challengeId, String userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        if (!"ACTIVE".equals(challenge.getStatus())) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_ACTIVE);
        }

        if (challenge.getEndTime() != null && challenge.getEndTime().isBefore(LocalDateTime.now())) {
            challenge.setStatus("EXPIRED");
            challengeRepository.save(challenge);
            throw new BusinessException(ErrorCode.CHALLENGE_EXPIRED);
        }

        // Use MongoTemplate to check if user already joined
        Query existsQuery = new Query(Criteria.where(FIELD_CHALLENGE_ID).is(challengeId).and(FIELD_USER_ID).is(userId));
        boolean alreadyJoined = mongoTemplate.exists(existsQuery, UserChallengeProgress.class);
        if (alreadyJoined) {
            throw new BusinessException(ErrorCode.CHALLENGE_ALREADY_JOINED);
        }

        UserChallengeProgress progress = new UserChallengeProgress();
        progress.setChallengeId(challengeId);
        progress.setUserId(userId);
        progress.setStatus(STATUS_IN_PROGRESS);
        progress.setJoinedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        UserChallengeProgress saved = userChallengeProgressRepository.save(progress);

        challenge.setParticipants(challenge.getParticipants() + 1);
        challenge.setUpdatedAt(LocalDateTime.now());
        challengeRepository.save(challenge);

        return saved;
    }

    @Override
    public void leaveChallenge(String challengeId, String userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        Query query = new Query(Criteria.where(FIELD_CHALLENGE_ID).is(challengeId).and(FIELD_USER_ID).is(userId));
        UserChallengeProgress progress = mongoTemplate.findOne(query, UserChallengeProgress.class);

        if (progress != null) {
            mongoTemplate.remove(progress);

            challenge.setParticipants(Math.max(0, challenge.getParticipants() - 1));
            challenge.setUpdatedAt(LocalDateTime.now());
            challengeRepository.save(challenge);
        }
    }

    @Override
    public List<UserChallengeProgressDTO> getChallengeParticipantsWithProgress(String challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        Query query = new Query(Criteria.where(FIELD_CHALLENGE_ID).is(challengeId));
        List<UserChallengeProgress> participants = mongoTemplate.find(query, UserChallengeProgress.class);
        List<UserChallengeProgressDTO> result = new ArrayList<>();

        for (UserChallengeProgress participant : participants) {
            UserChallengeProgressDTO dto = buildProgressDTO(participant, challenge);
            result.add(dto);
        }

        return result;
    }

    @Override
    public UserChallengeProgressDTO getUserChallengeProgress(String challengeId, String userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        // Use MongoTemplate with raw field names to find user progress
        Query query = new Query(Criteria.where(FIELD_CHALLENGE_ID).is(challengeId).and(FIELD_USER_ID).is(userId));
        UserChallengeProgress progress = mongoTemplate.findOne(query, UserChallengeProgress.class);
        if (progress == null) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        return buildProgressDTO(progress, challenge);
    }

    /**
     * Build user challenge progress DTO with real-time progress from Trip collection
     */
    private UserChallengeProgressDTO buildProgressDTO(UserChallengeProgress progress, Challenge challenge) {
        UserChallengeProgressDTO dto = new UserChallengeProgressDTO();
        
        populateBasicFields(dto, progress, challenge);
        populateUserInfo(dto, progress);
        calculateAndSetProgress(dto, progress, challenge);
        
        Double target = challenge.getTarget();
        Double current = dto.getCurrent();
        
        if (target != null && current >= target) {
            handleCompletedStatus(dto, progress, challenge);
        } else {
            handleInProgressStatus(dto, progress);
        }

        return dto;
    }

    private void populateBasicFields(UserChallengeProgressDTO dto, UserChallengeProgress progress, Challenge challenge) {
        dto.setId(progress.getId());
        dto.setChallengeId(progress.getChallengeId());
        dto.setUserId(progress.getUserId());
        dto.setJoinedAt(progress.getJoinedAt());
        dto.setCompletedAt(progress.getCompletedAt());
        dto.setRewardClaimed(progress.getRewardClaimed());
        dto.setTarget(challenge.getTarget());
    }

    private void populateUserInfo(UserChallengeProgressDTO dto, UserChallengeProgress progress) {
        User user = userRepository.findByUserid(progress.getUserId()).orElse(null);
        if (user != null) {
            dto.setUserNickname(user.getNickname());
            dto.setUserEmail(user.getEmail());
            dto.setUserAvatar(user.getAvatar());
        } else {
            dto.setUserNickname("Unknown User");
            dto.setUserEmail(null);
            dto.setUserAvatar(null);
        }
    }

    private void calculateAndSetProgress(UserChallengeProgressDTO dto, UserChallengeProgress progress, Challenge challenge) {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);
        Double current = calculateProgressFromTrips(
                progress.getUserId(),
                challenge.getType(),
                monthStart,
                monthEnd
        );
        dto.setCurrent(current);

        Double target = challenge.getTarget();
        if (target != null && target > 0) {
            dto.setProgressPercent(Math.min(100.0, (current / target) * 100));
        } else {
            dto.setProgressPercent(0.0);
        }
    }

    private void handleCompletedStatus(UserChallengeProgressDTO dto, UserChallengeProgress progress, Challenge challenge) {
        dto.setStatus(STATUS_COMPLETED);
        
        if (STATUS_IN_PROGRESS.equals(progress.getStatus())) {
            markChallengeAsCompletedFirstTime(dto, progress);
        } else if (Boolean.TRUE.equals(progress.getRewardClaimed())) {
            verifyAndFixRewardLog(dto, progress);
        }
    }

    private void markChallengeAsCompletedFirstTime(UserChallengeProgressDTO dto, UserChallengeProgress progress) {
        progress.setStatus(STATUS_COMPLETED);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());
        progress.setRewardClaimed(false);
        userChallengeProgressRepository.save(progress);
        dto.setCompletedAt(progress.getCompletedAt());
        dto.setRewardClaimed(false);
    }

    private void verifyAndFixRewardLog(UserChallengeProgressDTO dto, UserChallengeProgress progress) {
        List<com.example.EcoGo.model.UserPointsLog> logs =
                userPointsLogRepository.findByUserIdAndSource(progress.getUserId(), "challenges");
        boolean hasLog = logs.stream()
                .anyMatch(log -> progress.getChallengeId().equals(log.getRelatedId()));
        if (!hasLog) {
            progress.setRewardClaimed(false);
            progress.setUpdatedAt(LocalDateTime.now());
            userChallengeProgressRepository.save(progress);
            dto.setRewardClaimed(false);
        }
    }

    private void handleInProgressStatus(UserChallengeProgressDTO dto, UserChallengeProgress progress) {
        dto.setStatus(STATUS_IN_PROGRESS);
        
        if (STATUS_COMPLETED.equals(progress.getStatus()) && !Boolean.TRUE.equals(progress.getRewardClaimed())) {
            progress.setStatus(STATUS_IN_PROGRESS);
            progress.setCompletedAt(null);
            progress.setUpdatedAt(LocalDateTime.now());
            userChallengeProgressRepository.save(progress);
        }
    }

    @Override
    public UserChallengeProgressDTO claimChallengeReward(String challengeId, String userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        Query query = new Query(Criteria.where(FIELD_CHALLENGE_ID).is(challengeId).and(FIELD_USER_ID).is(userId));
        UserChallengeProgress progress = mongoTemplate.findOne(query, UserChallengeProgress.class);
        if (progress == null) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        if (!STATUS_COMPLETED.equals(progress.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Challenge not completed yet");
        }

        if (Boolean.TRUE.equals(progress.getRewardClaimed())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Reward already claimed");
        }

        // Award points via PointsService (logs to user_points_logs + updates currentPoints)
        if (challenge.getReward() != null && challenge.getReward() > 0) {
            pointsService.adjustPoints(
                    userId,
                    challenge.getReward().longValue(),
                    "challenges",
                    "Challenge reward: " + challenge.getTitle(),
                    challengeId,
                    null
            );
        }

        // Mark reward as claimed
        progress.setRewardClaimed(true);
        progress.setUpdatedAt(LocalDateTime.now());
        userChallengeProgressRepository.save(progress);

        return buildProgressDTO(progress, challenge);
    }

    /**
     * Calculate user progress from Trip collection using MongoTemplate aggregation.
     * Uses raw MongoDB field names to avoid @Field annotation mapping issues.
     */
    private Double calculateProgressFromTrips(String userId, String type, LocalDateTime startTime, LocalDateTime endTime) {
        Criteria criteria = Criteria.where(FIELD_USER_ID).is(userId)
                .and("is_green_trip").is(true)
                .and("carbon_status").is("completed")
                .and("start_time").gte(startTime).lt(endTime);

        switch (type) {
            case "GREEN_TRIPS_COUNT": {
                Query query = new Query(criteria);
                long count = mongoTemplate.count(query, TRIPS_COLLECTION);
                return (double) count;
            }

            case "GREEN_TRIPS_DISTANCE": {
                Aggregation aggregation = Aggregation.newAggregation(
                        Aggregation.match(criteria),
                        Aggregation.group().sum("distance").as(AGGREGATION_TOTAL_KEY)
                );
                AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, TRIPS_COLLECTION, Map.class);
                Map result = results.getUniqueMappedResult();
                if (result != null && result.get(AGGREGATION_TOTAL_KEY) != null) {
                    return ((Number) result.get(AGGREGATION_TOTAL_KEY)).doubleValue();
                }
                return 0.0;
            }

            case "CARBON_SAVED": {
                Aggregation aggregation = Aggregation.newAggregation(
                        Aggregation.match(criteria),
                        Aggregation.group().sum("carbon_saved").as(AGGREGATION_TOTAL_KEY)
                );
                AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, TRIPS_COLLECTION, Map.class);
                Map result = results.getUniqueMappedResult();
                if (result != null && result.get(AGGREGATION_TOTAL_KEY) != null) {
                    return ((Number) result.get(AGGREGATION_TOTAL_KEY)).doubleValue();
                }
                return 0.0;
            }

            default:
                return 0.0;
        }
    }
}
