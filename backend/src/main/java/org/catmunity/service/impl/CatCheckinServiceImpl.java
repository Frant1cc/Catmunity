package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.context.LoginContext;
import org.catmunity.exception.BusinessException;
import org.catmunity.mapper.CatCheckinMapper;
import org.catmunity.mapper.CatProfileMapper;
import org.catmunity.mapper.PostMapper;
import org.catmunity.model.dto.CatCheckinDTO;
import org.catmunity.model.entity.CatCheckin;
import org.catmunity.model.entity.CatProfile;
import org.catmunity.model.entity.Post;
import org.catmunity.model.vo.CatCheckinCalendarVO;
import org.catmunity.model.vo.CatCheckinVO;
import org.catmunity.result.PageResult;
import org.catmunity.service.CatCheckinService;
import org.catmunity.service.OSSService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatCheckinServiceImpl extends ServiceImpl<CatCheckinMapper, CatCheckin> implements CatCheckinService {

    private final OSSService ossService;
    private final CatProfileMapper catProfileMapper;
    private final PostMapper postMapper;

    @Override
    public CatCheckinVO checkin(CatCheckinDTO dto) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        if (dto.getCatProfileId() == null) {
            throw new BusinessException("请选择要打卡的猫咪");
        }

        CatProfile catProfile = catProfileMapper.selectById(dto.getCatProfileId());
        if (catProfile == null) {
            throw new BusinessException("猫咪档案不存在");
        }

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<CatCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CatCheckin::getUserId, userId)
                .eq(CatCheckin::getCatProfileId, dto.getCatProfileId())
                .eq(CatCheckin::getCheckinDate, today);
        CatCheckin existingCheckin = this.getOne(wrapper);
        if (existingCheckin != null) {
            throw new BusinessException("今日已打卡，请勿重复打卡");
        }

        CatCheckin checkin = CatCheckin.builder()
                .userId(userId)
                .catProfileId(dto.getCatProfileId())
                .checkinDate(today)
                .photoUrl(dto.getPhotoUrl())
                .remark(dto.getRemark())
                .syncedToPost(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        this.save(checkin);
        log.info("用户 {} 为猫咪 {} 打卡成功", userId, dto.getCatProfileId());

        CatCheckinVO vo = toVO(checkin);
        vo.setCatName(catProfile.getName());

        if (Boolean.TRUE.equals(dto.getSyncToPost())) {
            syncToPost(checkin.getId());
            vo.setSyncedToPost(true);
        }

        return vo;
    }

    @Override
    public CatCheckinVO getCheckinById(Long id) {
        CatCheckin checkin = this.getById(id);
        if (checkin == null) {
            throw new BusinessException("打卡记录不存在");
        }
        CatCheckinVO vo = toVO(checkin);
        CatProfile catProfile = catProfileMapper.selectById(checkin.getCatProfileId());
        if (catProfile != null) {
            vo.setCatName(catProfile.getName());
        }
        return vo;
    }

    @Override
    public List<CatCheckinVO> getTimeline(Long catProfileId) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        LambdaQueryWrapper<CatCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CatCheckin::getUserId, userId)
                .eq(catProfileId != null, CatCheckin::getCatProfileId, catProfileId)
                .orderByDesc(CatCheckin::getCheckinDate);

        List<CatCheckin> checkins = this.list(wrapper);

        List<Long> profileIds = checkins.stream()
                .map(CatCheckin::getCatProfileId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, CatProfile> profileMap = profileIds.isEmpty() ? Map.of() :
                catProfileMapper.selectBatchIds(profileIds).stream()
                        .collect(Collectors.toMap(CatProfile::getId, p -> p));

        return checkins.stream()
                .map(c -> {
                    CatCheckinVO vo = toVO(c);
                    CatProfile profile = profileMap.get(c.getCatProfileId());
                    if (profile != null) {
                        vo.setCatName(profile.getName());
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CatCheckinCalendarVO getCalendar(Long catProfileId, YearMonth yearMonth) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LambdaQueryWrapper<CatCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CatCheckin::getUserId, userId)
                .eq(catProfileId != null, CatCheckin::getCatProfileId, catProfileId)
                .ge(CatCheckin::getCheckinDate, startDate)
                .le(CatCheckin::getCheckinDate, endDate)
                .orderByAsc(CatCheckin::getCheckinDate);

        List<CatCheckin> checkins = this.list(wrapper);

        List<LocalDate> checkinDates = checkins.stream()
                .map(CatCheckin::getCheckinDate)
                .collect(Collectors.toList());

        int consecutiveDays = calculateConsecutiveDays(catProfileId, yearMonth);

        int daysInMonth = yearMonth.lengthOfMonth();
        boolean fullMonth = checkinDates.size() == daysInMonth;

        return CatCheckinCalendarVO.builder()
                .year(yearMonth.getYear())
                .month(yearMonth.getMonthValue())
                .checkinDates(checkinDates)
                .checkinCount(checkinDates.size())
                .consecutiveDays(consecutiveDays)
                .fullMonth(fullMonth)
                .build();
    }

    @Override
    public Integer getConsecutiveDays(Long catProfileId) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return 0;
        }

        LambdaQueryWrapper<CatCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CatCheckin::getUserId, userId)
                .eq(catProfileId != null, CatCheckin::getCatProfileId, catProfileId)
                .orderByDesc(CatCheckin::getCheckinDate);

        List<CatCheckin> checkins = this.list(wrapper);

        if (checkins.isEmpty()) {
            return 0;
        }

        int consecutive = 0;
        LocalDate today = LocalDate.now();
        LocalDate expectedDate = today;

        for (CatCheckin checkin : checkins) {
            if (checkin.getCheckinDate().equals(expectedDate)) {
                consecutive++;
                expectedDate = expectedDate.minusDays(1);
            } else if (checkin.getCheckinDate().equals(expectedDate.minusDays(1))) {
                expectedDate = checkin.getCheckinDate();
                consecutive++;
                expectedDate = expectedDate.minusDays(1);
            } else {
                break;
            }
        }

        return consecutive;
    }

    @Override
    public void syncToPost(Long checkinId) {
        CatCheckin checkin = this.getById(checkinId);
        if (checkin == null) {
            throw new BusinessException("打卡记录不存在");
        }

        if (checkin.getSyncedToPost() == 1) {
            throw new BusinessException("该打卡已同步到社区");
        }

        CatProfile catProfile = catProfileMapper.selectById(checkin.getCatProfileId());
        String catName = catProfile != null ? catProfile.getName() : "猫咪";

        String content = String.format("今日份的 %s 打卡完成！🐱 %s",
                catName,
                checkin.getRemark() != null ? checkin.getRemark() : "继续加油！");

        String photoUrl = checkin.getPhotoUrl();
        List<String> imageList = photoUrl != null ? List.of(photoUrl) : List.of();
        List<String> tagList = List.of("#猫咪打卡", "#" + catName);

        Post post = new Post();
        post.setUserId(checkin.getUserId());
        post.setCatProfileId(checkin.getCatProfileId());
        post.setContent(content);
        post.setImages(imageList);
        post.setTags(tagList);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setStatus(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        postMapper.insert(post);

        checkin.setSyncedToPost(1);
        checkin.setPostId(post.getId());
        checkin.setUpdatedAt(LocalDateTime.now());
        this.updateById(checkin);

        log.info("打卡 {} 已同步到帖子 {}", checkinId, post.getId());
    }

    private int calculateConsecutiveDays(Long catProfileId, YearMonth yearMonth) {
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate startDate = yearMonth.atDay(1);

        LambdaQueryWrapper<CatCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CatCheckin::getUserId, LoginContext.getUserId())
                .eq(catProfileId != null, CatCheckin::getCatProfileId, catProfileId)
                .le(CatCheckin::getCheckinDate, endDate)
                .orderByDesc(CatCheckin::getCheckinDate);

        List<CatCheckin> allCheckins = this.list(wrapper);

        if (allCheckins.isEmpty()) {
            return 0;
        }

        int consecutive = 0;
        LocalDate expectedDate = endDate.isAfter(LocalDate.now()) ? LocalDate.now() : endDate;

        for (CatCheckin checkin : allCheckins) {
            if (checkin.getCheckinDate().equals(expectedDate)) {
                consecutive++;
                expectedDate = expectedDate.minusDays(1);
                if (expectedDate.isBefore(startDate)) {
                    break;
                }
            } else if (checkin.getCheckinDate().isBefore(expectedDate)) {
                break;
            }
        }

        return consecutive;
    }

    private CatCheckinVO toVO(CatCheckin checkin) {
        CatCheckinVO vo = new CatCheckinVO();
        BeanUtils.copyProperties(checkin, vo);
        vo.setSyncedToPost(checkin.getSyncedToPost() != null && checkin.getSyncedToPost() == 1);
        return vo;
    }
}