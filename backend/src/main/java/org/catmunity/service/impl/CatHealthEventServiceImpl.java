package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.Enum.CatHealthEventTypeEnum;
import org.catmunity.exception.BusinessNotFoundException;
import org.catmunity.mapper.CatHealthEventMapper;
import org.catmunity.model.dto.CatHealthEventDTO;
import org.catmunity.model.entity.CatHealthEvent;
import org.catmunity.model.vo.CatHealthEventVO;
import org.catmunity.service.CatHealthEventService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatHealthEventServiceImpl extends ServiceImpl<CatHealthEventMapper, CatHealthEvent> implements CatHealthEventService {

    @Override
    public CatHealthEventVO createHealthEvent(CatHealthEventDTO dto) {
        CatHealthEvent event = new CatHealthEvent();
        BeanUtils.copyProperties(dto, event);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        save(event);
        log.info("创建健康事件成功：id={}, catId={}, type={}", event.getId(), event.getCatProfileId(), event.getEventType());
        return toVO(event);
    }

    @Override
    public CatHealthEventVO getHealthEventById(Long id) {
        CatHealthEvent event = getById(id);
        if (event == null) {
            throw new BusinessNotFoundException("健康事件不存在");
        }
        return toVO(event);
    }

    @Override
    public List<CatHealthEventVO> getEventsByCatProfileId(Long catProfileId) {
        List<CatHealthEvent> events = list(new LambdaQueryWrapper<CatHealthEvent>()
                .eq(CatHealthEvent::getCatProfileId, catProfileId)
                .orderByDesc(CatHealthEvent::getEventDate));
        return events.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<CatHealthEventVO> getTimelineByCatProfileId(Long catProfileId) {
        List<CatHealthEvent> events = list(new LambdaQueryWrapper<CatHealthEvent>()
                .eq(CatHealthEvent::getCatProfileId, catProfileId)
                .orderByAsc(CatHealthEvent::getEventDate));
        return events.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public CatHealthEventVO updateHealthEvent(CatHealthEventDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessNotFoundException("健康事件ID不能为空");
        }
        CatHealthEvent existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessNotFoundException("健康事件不存在");
        }
        BeanUtils.copyProperties(dto, existing, "id", "createdAt");
        updateById(existing);
        log.info("更新健康事件成功：id={}", existing.getId());
        return toVO(existing);
    }

    @Override
    public void deleteHealthEvent(Long id) {
        CatHealthEvent event = getById(id);
        if (event == null) {
            throw new BusinessNotFoundException("健康事件不存在");
        }
        removeById(id);
        log.info("删除健康事件成功：id={}", id);
    }

    @Override
    public List<CatHealthEventVO> getUpcomingReminders() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        List<CatHealthEvent> events = list(new LambdaQueryWrapper<CatHealthEvent>()
                .isNotNull(CatHealthEvent::getNextRemindDate)
                .ge(CatHealthEvent::getNextRemindDate, today)
                .le(CatHealthEvent::getNextRemindDate, nextWeek)
                .orderByAsc(CatHealthEvent::getNextRemindDate));
        return events.stream().map(this::toVO).collect(Collectors.toList());
    }

    private CatHealthEventVO toVO(CatHealthEvent event) {
        CatHealthEventVO vo = new CatHealthEventVO();
        BeanUtils.copyProperties(event, vo);
        vo.setEventTypeDesc(CatHealthEventTypeEnum.fromCode(event.getEventType()).getDescription());
        return vo;
    }
}