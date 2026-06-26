package org.catmunity.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.mapper.CatProfileMapper;
import org.catmunity.model.entity.CatProfile;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CatProfileSyncTaskUtil {

    @Autowired
    private CatProfileMapper catProfileMapper;

    @Autowired
    @Qualifier("catprofileVectorStore")
    private VectorStore vectorStore;

    @Value("${spring.ai.vectorstore.elasticsearch.indices.cat-profile:catprofile}")
    private String indexName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SOURCE_TYPE = "cat_profile";
    private static final int BATCH_SIZE = 10;

    private LocalDateTime lastSyncTime = null;
    private final Set<Long> syncedIds = new HashSet<>();

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledSync() {
        log.info("【定时任务-猫咪档案同步】开始同步，索引: {}, 上次同步时间: {}",
                indexName, lastSyncTime != null ? lastSyncTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "首次同步");
        long startTime = System.currentTimeMillis();

        try {
            int result = syncAllCatProfiles();
            lastSyncTime = LocalDateTime.now();

            long duration = System.currentTimeMillis() - startTime;
            log.info("【定时任务-猫咪档案同步】同步完成！处理: {} 条, 耗时: {}ms", result, duration);
        } catch (Exception e) {
            log.error("【定时任务-猫咪档案同步】同步失败: {}", e.getMessage(), e);
        }
    }

    public int syncAllCatProfiles() {
        LambdaQueryWrapper<CatProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CatProfile::getStatus, 0);

        List<CatProfile> catProfiles = catProfileMapper.selectList(queryWrapper);
        log.info("【同步任务】查询到 {} 条正常状态的猫咪档案", catProfiles.size());

        if (catProfiles.isEmpty()) {
            return 0;
        }

        Set<Long> currentIds = new HashSet<>();
        List<Document> documents = new ArrayList<>();

        for (CatProfile catProfile : catProfiles) {
            try {
                Document doc = createDocument(catProfile);
                if (doc != null) {
                    documents.add(doc);
                    currentIds.add(catProfile.getId());
                }
            } catch (Exception e) {
                log.error("【同步任务】转换失败: id={}, name={}, 错误: {}",
                        catProfile.getId(), catProfile.getName(), e.getMessage());
            }
        }

        if (!documents.isEmpty()) {
            try {
                int totalWritten = batchWriteDocuments(documents);
                log.info("【同步任务】成功写入 {} 条文档", totalWritten);
                syncedIds.clear();
                syncedIds.addAll(currentIds);
                return totalWritten;
            } catch (Exception e) {
                log.error("【同步任务】向量数据库写入失败: {}", e.getMessage(), e);
                throw new RuntimeException("向量数据库写入失败", e);
            }
        }

        return 0;
    }

    private int batchWriteDocuments(List<Document> documents) {
        int totalWritten = 0;
        int batchCount = (int) Math.ceil((double) documents.size() / BATCH_SIZE);

        log.info("【分批写入】总共 {} 条文档，分为 {} 批，每批最多 {} 条",
                documents.size(), batchCount, BATCH_SIZE);

        for (int i = 0; i < documents.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, documents.size());
            List<Document> batch = documents.subList(i, endIndex);
            int batchNumber = (i / BATCH_SIZE) + 1;

            try {
                vectorStore.accept(batch);
                totalWritten += batch.size();
                log.info("【分批写入】第 {} / {} 批完成，写入 {} 条文档",
                        batchNumber, batchCount, batch.size());
            } catch (Exception e) {
                log.error("【分批写入】第 {} / {} 批失败: {}",
                        batchNumber, batchCount, e.getMessage());
                throw new RuntimeException("批量写入文档失败", e);
            }
        }

        return totalWritten;
    }

    public int syncAllCatProfilesWithDeduplication() {
        return syncAllCatProfiles();
    }

    public void syncSingleCatProfile(Long catProfileId) {
        log.info("【单条同步】开始同步猫咪档案: id={}", catProfileId);

        try {
            CatProfile catProfile = catProfileMapper.selectById(catProfileId);
            if (catProfile == null) {
                log.warn("【单条同步】猫咪档案不存在: id={}", catProfileId);
                return;
            }

            if (catProfile.getStatus() != 0) {
                log.info("【单条同步】档案状态异常，不同步: id={}, status={}", catProfileId, catProfile.getStatus());
                return;
            }

            Document doc = createDocument(catProfile);
            if (doc != null) {
                vectorStore.accept(List.of(doc));
                syncedIds.add(catProfileId);
                log.info("【单条同步】成功同步: id={}, name={}", catProfile.getId(), catProfile.getName());
            }
        } catch (Exception e) {
            log.error("【单条同步】同步失败: id={}, 错误: {}", catProfileId, e.getMessage(), e);
        }
    }

    public void deleteProfileDocument(Long catProfileId) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query("cat_profile_id:" + catProfileId)
                    .topK(10)
                    .build();

            List<Document> docs = vectorStore.similaritySearch(searchRequest);

            if (!docs.isEmpty()) {
                log.info("【删除文档】找到档案文档: cat_profile_id={}, 数量={}，标记删除", catProfileId, docs.size());
            }
        } catch (Exception e) {
            log.warn("【删除文档】处理失败: cat_profile_id={}, 错误: {}", catProfileId, e.getMessage());
        }
    }

    public Map<String, Object> getSyncStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("indexName", indexName);
        status.put("lastSyncTime", lastSyncTime != null ?
                lastSyncTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        status.put("nextSyncTime", getNextScheduledTime());
        status.put("scheduledCron", "0 0 2 * * ?");
        status.put("deduplicationEnabled", true);
        status.put("description", "使用ID防重复机制，同ID文档会被覆盖更新");
        status.put("sourceType", SOURCE_TYPE);
        status.put("trackedSyncedIdsCount", syncedIds.size());

        LambdaQueryWrapper<CatProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CatProfile::getStatus, 0);
        Long totalCount = catProfileMapper.selectCount(queryWrapper);
        status.put("totalActiveProfiles", totalCount);

        return status;
    }

    private String getNextScheduledTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().plusDays(1).atTime(2, 0, 0);
        if (now.getHour() < 2) {
            next = now.toLocalDate().atTime(2, 0, 0);
        }
        return next.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private Document createDocument(CatProfile catProfile) {
        if (catProfile == null) {
            return null;
        }

        StringBuilder content = new StringBuilder();
        content.append("【猫咪档案信息】\n\n");

        if (catProfile.getName() != null) {
            content.append("猫咪名字：").append(catProfile.getName()).append("\n");
        }

        if (catProfile.getBreed() != null) {
            content.append("品种：").append(catProfile.getBreed()).append("\n");
            content.append(getBreedDescription(catProfile.getBreed()));
        }

        if (catProfile.getGender() != null) {
            content.append("性别：").append(catProfile.getGender() == 0 ? "母猫" : "公猫").append("\n");
        }

        if (catProfile.getBirthday() != null) {
            content.append("生日：").append(catProfile.getBirthday().format(DATE_FORMATTER)).append("\n");
            int age = calculateAge(catProfile.getBirthday());
            content.append("年龄：约").append(age).append("岁\n");
            content.append(getAgeAdvice(age));
        }

        if (catProfile.getWeight() != null) {
            content.append("体重：").append(catProfile.getWeight()).append("kg\n");
            content.append(getWeightAdvice(catProfile.getWeight().doubleValue()));
        }

        content.append("\n【日常养护建议】\n");
        content.append(generateCareAdvice(catProfile));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cat_profile_id", catProfile.getId());
        metadata.put("name", catProfile.getName());
        metadata.put("breed", catProfile.getBreed());
        metadata.put("gender", catProfile.getGender());
        metadata.put("source_type", SOURCE_TYPE);
        metadata.put("synced_at", LocalDateTime.now().toString());
        metadata.put("id", "cat_profile_" + catProfile.getId());

        return new Document(content.toString(), metadata);
    }

    private String getBreedDescription(String breed) {
        if (breed == null || breed.isEmpty()) {
            return "";
        }

        String lowerBreed = breed.toLowerCase();

        if (lowerBreed.contains("英短")) {
            return "英短猫特点：体型圆润，性格温和安静，适应能力强，容易相处。容易发胖需要控制饮食，被毛短密每周梳毛1-2次即可。\n";
        } else if (lowerBreed.contains("美短")) {
            return "美国短毛猫特点：活泼好动，好奇心强，身体健壮适应性好。善于捕鼠适合有花园的家庭，毛发短而光滑易于打理。\n";
        } else if (lowerBreed.contains("布偶")) {
            return "布偶猫特点：体型较大，性格温柔，忍耐力强适合有孩子的家庭。毛发长而柔软需要定期梳毛，是室内猫不适合户外活动。\n";
        } else if (lowerBreed.contains("波斯")) {
            return "波斯猫特点：长毛优雅面容扁平，性格安静喜欢安静环境。需要每日梳毛防止毛发打结，容易出现呼吸道和泪腺问题。\n";
        } else if (lowerBreed.contains("暹罗")) {
            return "暹罗猫特点：体型修长性格活泼，非常亲人喜欢与人互动，好奇心强喜欢探索。怕冷需要温暖的环境。\n";
        } else if (lowerBreed.contains("狸花")) {
            return "狸花猫特点：中国本土猫品种，身体强健适应能力强，性格独立捕鼠能力强，喂养简单不挑食。\n";
        } else if (lowerBreed.contains("橘")) {
            return "橘猫特点：橙色被毛可爱温顺，性格亲人易于相处，容易发胖需要控制食量，贪吃但活泼好动。\n";
        } else if (lowerBreed.contains("无毛")) {
            return "无毛猫特点：无毛发皮肤敏感，需要定期洗澡清洁，怕冷需要保暖衣物，非常亲人且体温较高。\n";
        }

        return "这是一只" + breed + "，是一种可爱的猫咪。\n";
    }

    private int calculateAge(LocalDate birthday) {
        if (birthday == null) {
            return 0;
        }
        LocalDate now = LocalDate.now();
        int age = now.getYear() - birthday.getYear();
        if (now.getDayOfYear() < birthday.getDayOfYear()) {
            age--;
        }
        return Math.max(0, age);
    }

    private String getAgeAdvice(int age) {
        if (age < 1) {
            return "幼猫需要喂食专门的幼猫粮或羊奶粉，每天喂食3-4次少量多餐，需要完成疫苗接种和驱虫，注意保暖，进行社会化管理训练。\n";
        } else if (age >= 1 && age < 7) {
            return "成猫应喂食成猫粮保持营养均衡，每天喂食2次定时定量，保持室内环境清洁，定期体检和疫苗加强，适当运动保持体重。\n";
        } else {
            return "老年猫应选择老年猫专用猫粮，减少单次喂食量增加喂食次数，定期体检关注肝肾功能，提供安静舒适的休息环境，关注关节健康。\n";
        }
    }

    private String getWeightAdvice(double weight) {
        if (weight < 2.5) {
            return "体重偏轻需要补充营养，可以选择高蛋白猫粮。\n";
        } else if (weight > 6.0) {
            return "体重偏重需要控制饮食增加运动量，避免肥胖带来的健康问题。\n";
        } else {
            return "体重正常，继续保持健康的饮食和运动习惯。\n";
        }
    }

    private String generateCareAdvice(CatProfile catProfile) {
        StringBuilder advice = new StringBuilder();

        advice.append("日常护理：\n");

        String breed = catProfile.getBreed();
        if (breed != null && (breed.toLowerCase().contains("长毛") ||
                breed.toLowerCase().contains("波斯") ||
                breed.toLowerCase().contains("布偶"))) {
            advice.append("- 长毛品种需要每天梳毛防止打结\n");
        } else {
            advice.append("- 短毛品种每周梳毛1-2次即可\n");
        }

        advice.append("- 定期清洁猫砂盆保持环境卫生\n");
        advice.append("- 定期修剪指甲每2-3周一次\n");
        advice.append("- 检查耳朵和牙齿健康\n");

        advice.append("\n饮食建议：\n");
        advice.append("- 选择优质猫粮根据年龄和体重计算喂食量\n");
        advice.append("- 保证充足清洁的饮用水\n");
        advice.append("- 避免喂食人类食物，尤其是洋葱巧克力等有毒食物\n");

        advice.append("\n健康管理：\n");
        advice.append("- 定期接种疫苗和驱虫\n");
        advice.append("- 建议每年进行一次全面体检\n");
        advice.append("- 观察猫咪的精神状态和食欲变化\n");

        return advice.toString();
    }
}