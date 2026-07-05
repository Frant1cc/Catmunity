package org.catmunity.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.model.dto.CatHealthEventDTO;
import org.catmunity.model.vo.CatHealthEventVO;
import org.catmunity.result.Result;
import org.catmunity.service.CatHealthEventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端-猫咪健康事件管理")
@RestController
@Slf4j
@RequestMapping("/catHealthEvent")
@RequiredArgsConstructor
public class CatHealthEventController {

    private final CatHealthEventService catHealthEventService;

    @Operation(summary = "创建健康事件")
    @PostMapping
    public Result<CatHealthEventVO> createHealthEvent(@RequestBody CatHealthEventDTO dto) {
        CatHealthEventVO vo = catHealthEventService.createHealthEvent(dto);
        return Result.success(vo);
    }

    @Operation(summary = "根据ID获取健康事件")
    @GetMapping("/{id}")
    public Result<CatHealthEventVO> getHealthEventById(@PathVariable Long id) {
        CatHealthEventVO vo = catHealthEventService.getHealthEventById(id);
        return Result.success(vo);
    }

    @Operation(summary = "获取猫咪的所有健康事件（按日期倒序）")
    @GetMapping("/cat/{catProfileId}")
    public Result<List<CatHealthEventVO>> getEventsByCatProfileId(@PathVariable Long catProfileId) {
        List<CatHealthEventVO> list = catHealthEventService.getEventsByCatProfileId(catProfileId);
        return Result.success(list);
    }

    @Operation(summary = "获取猫咪的健康时间线（按时间正序）")
    @GetMapping("/cat/{catProfileId}/timeline")
    public Result<List<CatHealthEventVO>> getTimelineByCatProfileId(@PathVariable Long catProfileId) {
        List<CatHealthEventVO> list = catHealthEventService.getTimelineByCatProfileId(catProfileId);
        return Result.success(list);
    }

    @Operation(summary = "更新健康事件")
    @PutMapping
    public Result<CatHealthEventVO> updateHealthEvent(@RequestBody CatHealthEventDTO dto) {
        CatHealthEventVO vo = catHealthEventService.updateHealthEvent(dto);
        return Result.success(vo);
    }

    @Operation(summary = "删除健康事件")
    @DeleteMapping("/{id}")
    public Result<?> deleteHealthEvent(@PathVariable Long id) {
        catHealthEventService.deleteHealthEvent(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取未来7天需要提醒的健康事件")
    @GetMapping("/reminders")
    public Result<List<CatHealthEventVO>> getUpcomingReminders() {
        List<CatHealthEventVO> list = catHealthEventService.getUpcomingReminders();
        return Result.success(list);
    }
}