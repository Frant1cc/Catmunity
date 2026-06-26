package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.CatCheckinDTO;
import org.catmunity.model.entity.CatCheckin;
import org.catmunity.model.vo.CatCheckinCalendarVO;
import org.catmunity.model.vo.CatCheckinVO;

import java.time.YearMonth;
import java.util.List;

public interface CatCheckinService extends IService<CatCheckin> {

    CatCheckinVO checkin(CatCheckinDTO dto);

    CatCheckinVO getCheckinById(Long id);

    List<CatCheckinVO> getTimeline(Long catProfileId);

    CatCheckinCalendarVO getCalendar(Long catProfileId, YearMonth yearMonth);

    Integer getConsecutiveDays(Long catProfileId);

    void syncToPost(Long checkinId);
}