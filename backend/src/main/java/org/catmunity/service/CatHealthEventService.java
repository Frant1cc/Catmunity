package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.CatHealthEventDTO;
import org.catmunity.model.entity.CatHealthEvent;
import org.catmunity.model.vo.CatHealthEventVO;

import java.util.List;

public interface CatHealthEventService extends IService<CatHealthEvent> {

    CatHealthEventVO createHealthEvent(CatHealthEventDTO dto);

    CatHealthEventVO getHealthEventById(Long id);

    List<CatHealthEventVO> getEventsByCatProfileId(Long catProfileId);

    List<CatHealthEventVO> getTimelineByCatProfileId(Long catProfileId);

    CatHealthEventVO updateHealthEvent(CatHealthEventDTO dto);

    void deleteHealthEvent(Long id);

    List<CatHealthEventVO> getUpcomingReminders();
}