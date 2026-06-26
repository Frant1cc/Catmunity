package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.constants.SystemConstant;
import org.catmunity.context.LoginContext;
import org.catmunity.exception.BusinessException;
import org.catmunity.mapper.ModerationRecordMapper;
import org.catmunity.mapper.PostMapper;
import org.catmunity.mapper.CommentMapper;
import org.catmunity.model.dto.ModerationActionDTO;
import org.catmunity.model.dto.ModerationQueryDTO;
import org.catmunity.model.entity.ModerationRecord;
import org.catmunity.model.entity.Post;
import org.catmunity.model.entity.User;
import org.catmunity.Enum.ContentType;
import org.catmunity.Enum.ModerationStatus;
import org.catmunity.Enum.RiskLevel;
import org.catmunity.Enum.ViolationType;
import org.catmunity.model.vo.ModerationRecordVO;
import org.catmunity.result.PageResult;
import org.catmunity.service.ModerationService;
import org.catmunity.service.UserService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationServiceImpl extends ServiceImpl<ModerationRecordMapper, ModerationRecord> implements ModerationService {

    private final UserService userService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final ChatClient moderatorChatClient;

    private static final double RISK_THRESHOLD = 0.5;
    private static final double HIGH_RISK_THRESHOLD = 0.7;



    @Override
    public PageResult<ModerationRecordVO> getPendingList(ModerationQueryDTO queryDTO, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ModerationRecord> wrapper = buildQueryWrapper(queryDTO);
        wrapper.in(queryDTO.getStatus() == null, ModerationRecord::getStatus,
                ModerationStatus.PENDING.getCode(), ModerationStatus.AUTO_FLAGGED.getCode());

        wrapper.orderByAsc(ModerationRecord::getRiskLevel)
                .orderByDesc(ModerationRecord::getAiRiskScore)
                .orderByAsc(ModerationRecord::getCreatedAt);

        Page<ModerationRecord> page = new Page<>(pageNum, pageSize);
        page = page(page, wrapper);

        List<ModerationRecordVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), page.getPages(), pageNum, pageSize, voList);
    }

    @Override
    public PageResult<ModerationRecordVO> getModerationLogs(ModerationQueryDTO queryDTO, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ModerationRecord> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(ModerationRecord::getReviewedAt);

        Page<ModerationRecord> page = new Page<>(pageNum, pageSize);
        page = page(page, wrapper);

        List<ModerationRecordVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), page.getPages(), pageNum, pageSize, voList);
    }

    @Override
    public ModerationRecordVO getModerationDetail(Long id) {
        ModerationRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException("审核记录不存在");
        }
        return toVO(record);
    }

    @Override
    @Transactional
    public void approveContent(Long moderationId, String comment) {
        ModerationRecord record = this.getById(moderationId);
        if (record == null) {
            throw new BusinessException("审核记录不存在");
        }
        if (record.getStatus() == ModerationStatus.APPROVED.getCode()) {
            throw new BusinessException("内容已通过审核");
        }

        LoginContext.LoginUserDTO admin = LoginContext.getUser();
        User user = userService.getById(admin.getUserId());

        record.setStatus(ModerationStatus.APPROVED.getCode());
        record.setReviewerId(admin.getUserId());
        record.setReviewerName(user.getIdentifier());
        record.setReviewComment(comment);
        record.setReviewedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        this.updateById(record);
        log.info("管理员 {} 通过内容审核，记录ID: {}", user.getIdentifier(), moderationId);
    }

    @Override
    @Transactional
    public void rejectContent(Long moderationId, String reason) {
        ModerationRecord record = this.getById(moderationId);
        if (record == null) {
            throw new BusinessException("审核记录不存在");
        }
        if (record.getStatus() == ModerationStatus.REJECTED.getCode()) {
            throw new BusinessException("内容已拒绝");
        }

        LoginContext.LoginUserDTO admin = LoginContext.getUser();
        User user = userService.getById(admin.getUserId());

        record.setStatus(ModerationStatus.REJECTED.getCode());
        record.setReviewerId(admin.getUserId());
        record.setReviewerName(user.getIdentifier());
        record.setReviewComment(reason);
        record.setReviewedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        this.updateById(record);

        updateContentStatus(Long.valueOf(record.getContentType()), record.getContentId(), 2);

        log.info("管理员 {} 拒绝内容审核，记录ID: {}，原因: {}", user.getIdentifier(), moderationId, reason);
    }

    @Override
    @Transactional
    public void batchApprove(List<Long> moderationIds) {
        LoginContext.LoginUserDTO admin = LoginContext.getUser();
        User user = userService.getById(admin.getUserId());
        for (Long id : moderationIds) {
            try {
                approveContent(id, "批量通过");
            } catch (Exception e) {
                log.error("批量通过审核记录 {} 失败", id, e);
            }
        }
        log.info("管理员 {} 批量通过 {} 条审核记录", user.getIdentifier(), moderationIds.size());
    }

    @Override
    @Transactional
    public void batchReject(List<Long> moderationIds, String reason) {
        LoginContext.LoginUserDTO admin = LoginContext.getUser();
        User user = userService.getById(admin.getUserId());
        for (Long id : moderationIds) {
            try {
                rejectContent(id, reason);
            } catch (Exception e) {
                log.error("批量拒绝审核记录 {} 失败", id, e);
            }
        }
        log.info("管理员 {} 批量拒绝 {} 条审核记录，原因: {}", user.getIdentifier(), moderationIds.size(), reason);
    }

    @Override
    @Transactional
    public void autoModerate(Long contentType, Long contentId, String content, Long authorId) {
        try {
            // 调用AI大模型进行内容审核
            AI moderationResult = callAIModeration(content);
            
            String summary = content.length() > 100 ? content.substring(0, 100) + "..." : content;
            
            ModerationRecord record = ModerationRecord.builder()
                    .contentType(contentType.intValue())
                    .contentId(contentId)
                    .contentSummary(summary)
                    .originalContent(content)
                    .authorId(authorId)
                    .aiRiskScore(moderationResult.riskScore)
                    .riskLevel(moderationResult.riskLevel)
                    .violationTypes(moderationResult.violationTypes.isEmpty() ? null : String.join(",", moderationResult.violationTypes))
                    .aiAnalysis(moderationResult.analysis)
                    .status(moderationResult.riskScore >= HIGH_RISK_THRESHOLD ?
                            ModerationStatus.AUTO_FLAGGED.getCode() : ModerationStatus.PENDING.getCode())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            this.save(record);
            log.info("AI大模型审核内容 {}:{}，风险评分: {}，风险等级: {}", contentType, contentId, moderationResult.riskScore, moderationResult.riskLevel);
        } catch (Exception e) {
            log.error("AI审核调用失败，使用默认审核逻辑", e);
            // AI调用失败时使用规则审核作为兜底
            fallbackRuleModerate(contentType, contentId, content, authorId);
        }
    }

    private AI callAIModeration(String content) {
        try {
            PromptTemplate promptTemplate = new PromptTemplate(SystemConstant.MODERATION_PROMPT);
            Prompt prompt = promptTemplate.create(Map.of("content", content));
            
            String response = moderatorChatClient.prompt(prompt)
                    .call()
                    .content();
            
            log.info("AI审核响应: {}", response);
            
            return parseAIResponse(response);
        } catch (Exception e) {
            log.error("调用AI审核失败: {}", e.getMessage());
            throw new RuntimeException("AI审核服务调用失败", e);
        }
    }

    private AI parseAIResponse(String response) {
        AI result = new AI();
        result.riskScore = 0.0;
        result.riskLevel = 0;
        result.violationTypes = new ArrayList<>();
        result.analysis = "";
        
        try {
            // 解析JSON响应
            response = response.replaceAll("```json", "").replaceAll("```", "").trim();
            
            // 提取风险评分
            if (response.contains("riskScore")) {
                String scoreStr = response.contains("riskScore\":") ? 
                        response.split("riskScore\":")[1].split(",")[0].replaceAll("[^0-9.]", "") : "0";
                result.riskScore = Double.parseDouble(scoreStr);
            }
            
            // 提取风险等级
            if (response.contains("riskLevel")) {
                String level = response.split("riskLevel")[1].split(",")[0].replaceAll("[^\\u4e00-\\u9fa5a-zA-Z]", "");
                result.riskLevel = parseRiskLevel(level);
            }
            
            // 提取违规类型
            if (response.contains("violationTypes")) {
                String violationsPart = response.split("violationTypes")[1];
                if (violationsPart.contains("[\"")) {
                    String[] types = violationsPart.split("[\\[\\]\"]");
                    for (String type : types) {
                        type = type.trim();
                        if (!type.isEmpty() && !type.equals("violationTypes") && !type.equals(":")) {
                            result.violationTypes.add(type.replaceAll("\"", "").trim());
                        }
                    }
                }
            }
            
            // 提取分析说明
            if (response.contains("analysis")) {
                result.analysis = response.split("analysis")[1]
                        .replaceAll("^\"", "")
                        .replaceAll("\"?\\s*$", "")
                        .trim();
            }
            
            // 如果没有检测到违规但评分较高，给予默认值
            if (result.violationTypes.isEmpty() && result.riskScore >= HIGH_RISK_THRESHOLD) {
                result.riskLevel = parseRiskLevel(getRiskLevelDescription(result.riskScore));
            }
            
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", e.getMessage());
        }
        
        return result;
    }

    private int parseRiskLevel(String level) {
        if (level.contains("极高") || level.contains("高")) return 4;
        if (level.contains("高")) return 3;
        if (level.contains("中")) return 2;
        if (level.contains("低")) return 1;
        return 0;
    }

    private String getRiskLevelDescription(double score) {
        if (score >= 0.8) return "极高风险";
        if (score >= 0.6) return "高风险";
        if (score >= 0.4) return "中风险";
        if (score >= 0.2) return "低风险";
        return "安全";
    }

    private void fallbackRuleModerate(Long contentType, Long contentId, String content, Long authorId) {
        double riskScore = calculateRiskScore(content);
        RiskLevel riskLevel = RiskLevel.fromScore(riskScore);
        List<String> violations = detectViolations(content);

        String summary = content.length() > 100 ? content.substring(0, 100) + "..." : content;

        ModerationRecord record = ModerationRecord.builder()
                .contentType(contentType.intValue())
                .contentId(contentId)
                .contentSummary(summary)
                .originalContent(content)
                .authorId(authorId)
                .aiRiskScore(riskScore)
                .riskLevel(riskLevel.getCode())
                .violationTypes(violations.isEmpty() ? null : String.join(",", violations))
                .aiAnalysis(generateAIAnalysis(riskScore, violations))
                .status(riskScore >= HIGH_RISK_THRESHOLD ?
                        ModerationStatus.AUTO_FLAGGED.getCode() : ModerationStatus.PENDING.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        this.save(record);
        log.info("规则审核兜底内容 {}:{}，风险评分: {}，风险等级: {}", contentType, contentId, riskScore, riskLevel);
    }

    private static class AI {
        double riskScore;
        int riskLevel;
        List<String> violationTypes;
        String analysis;
    }

    private LambdaQueryWrapper<ModerationRecord> buildQueryWrapper(ModerationQueryDTO queryDTO) {
        LambdaQueryWrapper<ModerationRecord> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getContentType() != null) {
            wrapper.eq(ModerationRecord::getContentType, queryDTO.getContentType());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(ModerationRecord::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getRiskLevel() != null) {
            wrapper.eq(ModerationRecord::getRiskLevel, queryDTO.getRiskLevel());
        }
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(ModerationRecord::getCreatedAt, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(ModerationRecord::getCreatedAt, queryDTO.getEndTime());
        }
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(ModerationRecord::getContentSummary, queryDTO.getKeyword())
                    .or().like(ModerationRecord::getOriginalContent, queryDTO.getKeyword()));
        }

        return wrapper;
    }

    private ModerationRecordVO toVO(ModerationRecord record) {
        ModerationRecordVO vo = new ModerationRecordVO();
        BeanUtils.copyProperties(record, vo);

        vo.setContentTypeStr(ContentType.fromCode(record.getContentType()) != null ?
                ContentType.fromCode(record.getContentType()).getDescription() : "未知");

        RiskLevel risk = RiskLevel.fromCode(record.getRiskLevel());
        vo.setRiskLevelStr(risk != null ? risk.getDescription() : "未知");
        vo.setRiskLevelColor(risk != null ? risk.getColor() : "#999999");

        vo.setStatusStr(ModerationStatus.fromCode(record.getStatus()) != null ?
                ModerationStatus.fromCode(record.getStatus()).getDescription() : "未知");

        if (record.getViolationTypes() != null && !record.getViolationTypes().isEmpty()) {
            vo.setViolationTypes(Arrays.asList(record.getViolationTypes().split(",")));
        } else {
            vo.setViolationTypes(new ArrayList<>());
        }

        return vo;
    }

    private double calculateRiskScore(String content) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }

        String lowerContent = content.toLowerCase();
        double score = 0.0;

        String[] pornKeywords = {"色情", "黄色", "裸", "性感", " porn", "sex"};
        String[] violenceKeywords = {"暴力", "血腥", "杀人", "死亡", " violence"};
        String[] adKeywords = {"广告", "推广", "微信", "QQ号", "购买", "链接"};

        for (String kw : pornKeywords) {
            if (lowerContent.contains(kw.toLowerCase())) score += 0.3;
        }
        for (String kw : violenceKeywords) {
            if (lowerContent.contains(kw.toLowerCase())) score += 0.25;
        }
        for (String kw : adKeywords) {
            if (lowerContent.contains(kw.toLowerCase())) score += 0.2;
        }

        if (content.length() < 5) score += 0.1;

        return Math.min(score, 1.0);
    }

    private List<String> detectViolations(String content) {
        List<String> violations = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return violations;
        }

        String lowerContent = content.toLowerCase();

        if (containsAny(lowerContent, "色情", "黄色", "裸", "porn", "sex")) {
            violations.add(ViolationType.PORNOGRAPHY.getDescription());
        }
        if (containsAny(lowerContent, "暴力", "血腥", "杀人")) {
            violations.add(ViolationType.VIOLENCE.getDescription());
        }
        if (containsAny(lowerContent, "广告", "推广", "微信", "QQ")) {
            violations.add(ViolationType.ADVERTISING.getDescription());
        }
        if (containsAny(lowerContent, "骂", "滚", "垃圾")) {
            violations.add(ViolationType.PERSONAL_ATTACK.getDescription());
        }

        return violations;
    }

    private boolean containsAny(String content, String... keywords) {
        for (String kw : keywords) {
            if (content.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String generateAIAnalysis(double riskScore, List<String> violations) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("风险评分: %.2f\n", riskScore));
        if (!violations.isEmpty()) {
            sb.append("检测到可能的违规类型: ");
            sb.append(String.join(", ", violations));
        } else {
            sb.append("未检测到明显违规内容");
        }
        if (riskScore >= HIGH_RISK_THRESHOLD) {
            sb.append("\n建议: 人工复核");
        }
        return sb.toString();
    }

    private void updateContentStatus(Long contentType, Long contentId, Integer status) {
        if (contentType == 1) {
            Post post = postMapper.selectById(contentId);
            if (post != null) {
                post.setStatus(status);
                postMapper.updateById(post);
            }
        }
    }
}