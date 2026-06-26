package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.mapper.*;
import org.catmunity.model.entity.*;
import org.catmunity.model.vo.*;
import org.catmunity.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CatCheckinMapper catCheckinMapper;
    private final CatProfileMapper catProfileMapper;
    private final CommentMapper commentMapper;
    private final ModerationRecordMapper moderationRecordMapper;
    private final LogMapper logMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public DashboardOverviewVO getOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekAgo = LocalDate.now().minusDays(7).atStartOfDay();

        Long totalUsers = userMapper.selectCount(null);
        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, todayStart)
        );

        Long activeUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getLastActive, weekAgo)
        );

        Long totalPosts = postMapper.selectCount(null);
        Long todayNewPosts = postMapper.selectCount(
                new LambdaQueryWrapper<Post>().ge(Post::getCreatedAt, todayStart)
        );

        Long totalCatProfiles = catProfileMapper.selectCount(null);
        Long totalCheckins = catCheckinMapper.selectCount(null);
        Long todayNewCheckins = catCheckinMapper.selectCount(
                new LambdaQueryWrapper<CatCheckin>().ge(CatCheckin::getCreatedAt, todayStart)
        );

        Long pendingModerationCount = moderationRecordMapper.selectCount(
                new LambdaQueryWrapper<ModerationRecord>().eq(ModerationRecord::getStatus, 0)
        );

        return DashboardOverviewVO.builder()
                .totalUsers(totalUsers)
                .todayNewUsers(todayNewUsers)
                .activeUsers(activeUsers)
                .totalPosts(totalPosts)
                .todayNewPosts(todayNewPosts)
                .totalCatProfiles(totalCatProfiles)
                .totalCheckins(totalCheckins)
                .todayNewCheckins(todayNewCheckins)
                .pendingModerationCount(pendingModerationCount)
                .build();
    }

    @Override
    public UserGrowthTrendVO getUserGrowthTrend(String dimension, Integer days) {
        if (days == null || days <= 0) {
            days = 30;
        }
        if (dimension == null) {
            dimension = "day";
        }

        LocalDateTime startDate = getStartDate(dimension, days);
        List<User> allUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, startDate)
        );

        List<TrendDataVO> trendData = new ArrayList<>();
        Map<String, Long> groupedData = new LinkedHashMap<>();

        if ("day".equals(dimension)) {
            groupedData = allUsers.stream()
                    .collect(Collectors.groupingBy(
                            u -> toLocalDateTime(u.getCreatedAt()).format(DATE_FORMATTER),
                            LinkedHashMap::new,
                            Collectors.counting()
                    ));
        } else if ("week".equals(dimension)) {
            groupedData = allUsers.stream()
                    .collect(Collectors.groupingBy(
                            u -> getWeekKey(toLocalDateTime(u.getCreatedAt())),
                            LinkedHashMap::new,
                            Collectors.counting()
                    ));
        } else if ("month".equals(dimension)) {
            groupedData = allUsers.stream()
                    .collect(Collectors.groupingBy(
                            u -> toLocalDateTime(u.getCreatedAt()).format(DateTimeFormatter.ofPattern("yyyy-MM")),
                            LinkedHashMap::new,
                            Collectors.counting()
                    ));
        }

        LocalDateTime current = startDate;
        LocalDateTime now = LocalDateTime.now();
        while (current.isBefore(now)) {
            String key = getKeyForDimension(dimension, current);
            groupedData.putIfAbsent(key, 0L);
            current = incrementDate(current, dimension);
        }

        groupedData.forEach((date, count) -> trendData.add(
                TrendDataVO.builder().date(date).count(count).build()
        ));

        trendData.sort(Comparator.comparing(TrendDataVO::getDate));

        long totalGrowth = trendData.stream().mapToLong(TrendDataVO::getCount).sum();

        return UserGrowthTrendVO.builder()
                .dimension(dimension)
                .data(trendData)
                .totalGrowth(totalGrowth)
                .build();
    }

    @Override
    public ContentStatsVO getContentStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekAgo = LocalDate.now().minusDays(7).atStartOfDay();

        Long totalPosts = postMapper.selectCount(null);
        Long todayNewPosts = postMapper.selectCount(
                new LambdaQueryWrapper<Post>().ge(Post::getCreatedAt, todayStart)
        );

        Long totalCheckins = catCheckinMapper.selectCount(null);
        Long todayNewCheckins = catCheckinMapper.selectCount(
                new LambdaQueryWrapper<CatCheckin>().ge(CatCheckin::getCreatedAt, todayStart)
        );

        Long totalComments = commentMapper.selectCount(null);
        Long todayNewComments = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().ge(Comment::getCreatedAt, todayStart)
        );

        List<TrendDataVO> trendData = generateContentTrend(weekAgo);

        return ContentStatsVO.builder()
                .totalPosts(totalPosts)
                .todayNewPosts(todayNewPosts)
                .totalCheckins(totalCheckins)
                .todayNewCheckins(todayNewCheckins)
                .totalComments(totalComments)
                .todayNewComments(todayNewComments)
                .trendData(trendData)
                .build();
    }

    @Override
    public AIFeatureUsageVO getAIFeatureUsage() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekAgo = LocalDate.now().minusDays(7).atStartOfDay();

        Long chatCount = logMapper.selectCount(
                new LambdaQueryWrapper<Log>().eq(Log::getMessageType, "USER")
        );
        Long todayChatCount = logMapper.selectCount(
                new LambdaQueryWrapper<Log>()
                        .eq(Log::getMessageType, "USER")
                        .ge(Log::getCreatedAt, todayStart)
        );

        Long imageRecognitionCount = logMapper.selectCount(
                new LambdaQueryWrapper<Log>()
                        .eq(Log::getMessageType, "USER")
                        .like(Log::getMetadata, "image")
        );
        Long todayImageRecognitionCount = logMapper.selectCount(
                new LambdaQueryWrapper<Log>()
                        .eq(Log::getMessageType, "USER")
                        .like(Log::getMetadata, "image")
                        .ge(Log::getCreatedAt, todayStart)
        );

        Long summaryGenerationCount = logMapper.selectCount(
                new LambdaQueryWrapper<Log>()
                        .eq(Log::getMessageType, "USER")
                        .like(Log::getMetadata, "summary")
        );
        Long todaySummaryGenerationCount = logMapper.selectCount(
                new LambdaQueryWrapper<Log>()
                        .eq(Log::getMessageType, "USER")
                        .like(Log::getMetadata, "summary")
                        .ge(Log::getCreatedAt, todayStart)
        );

        List<TrendDataVO> trendData = generateAITrend(weekAgo);

        return AIFeatureUsageVO.builder()
                .chatCount(chatCount)
                .imageRecognitionCount(imageRecognitionCount)
                .summaryGenerationCount(summaryGenerationCount)
                .todayChatCount(todayChatCount)
                .todayImageRecognitionCount(todayImageRecognitionCount)
                .todaySummaryGenerationCount(todaySummaryGenerationCount)
                .trendData(trendData)
                .build();
    }

    @Override
    public ModerationStatsVO getModerationStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        Long pendingCount = moderationRecordMapper.selectCount(
                new LambdaQueryWrapper<ModerationRecord>().eq(ModerationRecord::getStatus, 0)
        );
        Long approvedCount = moderationRecordMapper.selectCount(
                new LambdaQueryWrapper<ModerationRecord>().eq(ModerationRecord::getStatus, 1)
        );
        Long rejectedCount = moderationRecordMapper.selectCount(
                new LambdaQueryWrapper<ModerationRecord>().eq(ModerationRecord::getStatus, 2)
        );
        Long autoFlaggedCount = moderationRecordMapper.selectCount(
                new LambdaQueryWrapper<ModerationRecord>().eq(ModerationRecord::getStatus, 3)
        );

        Long todayModeratedCount = moderationRecordMapper.selectCount(
                new LambdaQueryWrapper<ModerationRecord>().ge(ModerationRecord::getReviewedAt, todayStart)
        );
        Long todayApprovedCount = moderationRecordMapper.selectCount(
                new LambdaQueryWrapper<ModerationRecord>()
                        .eq(ModerationRecord::getStatus, 1)
                        .ge(ModerationRecord::getReviewedAt, todayStart)
        );
        Long todayRejectedCount = moderationRecordMapper.selectCount(
                new LambdaQueryWrapper<ModerationRecord>()
                        .eq(ModerationRecord::getStatus, 2)
                        .ge(ModerationRecord::getReviewedAt, todayStart)
        );

        double passRate = 0.0;
        long totalReviewed = approvedCount + rejectedCount;
        if (totalReviewed > 0) {
            passRate = (double) approvedCount / totalReviewed * 100;
        }

        return ModerationStatsVO.builder()
                .pendingCount(pendingCount)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .autoFlaggedCount(autoFlaggedCount)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .todayModeratedCount(todayModeratedCount)
                .todayApprovedCount(todayApprovedCount)
                .todayRejectedCount(todayRejectedCount)
                .build();
    }

    @Override
    public CatBreedDistributionVO getCatBreedDistribution() {
        List<CatProfile> allCats = catProfileMapper.selectList(null);

        Map<String, Long> breedCountMap = allCats.stream()
                .collect(Collectors.groupingBy(
                        cat -> cat.getBreed() != null && !cat.getBreed().isEmpty() ? cat.getBreed() : "未知",
                        Collectors.counting()
                ));

        long totalCats = allCats.size();
        long unknownBreedCount = breedCountMap.getOrDefault("未知", 0L);

        List<CatBreedDistributionVO.BreedDataVO> breeds = breedCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> CatBreedDistributionVO.BreedDataVO.builder()
                        .breed(entry.getKey())
                        .count(entry.getValue())
                        .percentage(totalCats > 0 ? Math.round(entry.getValue() * 100.0 / totalCats * 100.0) / 100.0 : 0.0)
                        .build())
                .collect(Collectors.toList());

        return CatBreedDistributionVO.builder()
                .breeds(breeds)
                .totalCats(totalCats)
                .breedCount(breedCountMap.size())
                .unknownBreedCount(unknownBreedCount)
                .build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private LocalDateTime getStartDate(String dimension, Integer days) {
        LocalDate now = LocalDate.now();
        return switch (dimension) {
            case "week" -> now.minusWeeks(days).atStartOfDay();
            case "month" -> now.minusMonths(days).atStartOfDay();
            default -> now.minusDays(days).atStartOfDay();
        };
    }

    private String getWeekKey(LocalDateTime dateTime) {
        LocalDate weekStart = dateTime.toLocalDate().with(DayOfWeek.MONDAY);
        return weekStart.format(DATE_FORMATTER);
    }

    private String getKeyForDimension(String dimension, LocalDateTime dateTime) {
        return switch (dimension) {
            case "week" -> getWeekKey(dateTime);
            case "month" -> dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            default -> dateTime.format(DATE_FORMATTER);
        };
    }

    private LocalDateTime incrementDate(LocalDateTime current, String dimension) {
        return switch (dimension) {
            case "week" -> current.plusWeeks(1);
            case "month" -> current.plusMonths(1);
            default -> current.plusDays(1);
        };
    }

    private List<TrendDataVO> generateContentTrend(LocalDateTime startDate) {
        List<TrendDataVO> trendData = new ArrayList<>();
        LocalDateTime current = startDate;
        LocalDateTime now = LocalDateTime.now();

        while (current.isBefore(now)) {
            String dateStr = current.format(DATE_FORMATTER);
            LocalDateTime dayStart = current.toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = current.toLocalDate().atTime(LocalTime.MAX);

            Long posts = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>()
                            .ge(Post::getCreatedAt, dayStart)
                            .le(Post::getCreatedAt, dayEnd)
            );
            Long checkins = catCheckinMapper.selectCount(
                    new LambdaQueryWrapper<CatCheckin>()
                            .ge(CatCheckin::getCreatedAt, dayStart)
                            .le(CatCheckin::getCreatedAt, dayEnd)
            );
            Long comments = commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>()
                            .ge(Comment::getCreatedAt, dayStart)
                            .le(Comment::getCreatedAt, dayEnd)
            );

            trendData.add(TrendDataVO.builder()
                    .date(dateStr)
                    .count(posts + checkins + comments)
                    .build());

            current = current.plusDays(1);
        }

        return trendData;
    }

    private List<TrendDataVO> generateAITrend(LocalDateTime startDate) {
        List<TrendDataVO> trendData = new ArrayList<>();
        LocalDateTime current = startDate;
        LocalDateTime now = LocalDateTime.now();

        while (current.isBefore(now)) {
            String dateStr = current.format(DATE_FORMATTER);
            LocalDateTime dayStart = current.toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = current.toLocalDate().atTime(LocalTime.MAX);

            Long chatCount = logMapper.selectCount(
                    new LambdaQueryWrapper<Log>()
                            .eq(Log::getMessageType, "USER")
                            .ge(Log::getCreatedAt, dayStart)
                            .le(Log::getCreatedAt, dayEnd)
            );

            trendData.add(TrendDataVO.builder()
                    .date(dateStr)
                    .count(chatCount)
                    .build());

            current = current.plusDays(1);
        }

        return trendData;
    }
}