package org.catmunity.controller.user;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.json.JacksonObjectMapper;
import org.catmunity.model.dto.CatCheckinDTO;
import org.catmunity.model.vo.CatCheckinCalendarVO;
import org.catmunity.model.vo.CatCheckinVO;
import org.catmunity.result.Result;
import org.catmunity.service.CatCheckinService;
import org.catmunity.service.OSSService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "用户端-猫咪打卡")
@RestController
@RequestMapping("/user/checkin")
@RequiredArgsConstructor
@Slf4j
public class CatCheckinController {

    private final CatCheckinService catCheckinService;
    private final OSSService ossService;

    @Operation(summary = "猫咪打卡")
    @PostMapping("/checkin")
    public Result<CatCheckinVO> checkin(
            @RequestParam Long catProfileId,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String photoURL,
            @RequestParam(required = false, defaultValue = "false") Boolean syncToPost) {

        CatCheckinDTO dto = CatCheckinDTO.builder()
                .catProfileId(catProfileId)
                .remark(remark)
                .photoUrl(photoURL)
                .syncToPost(syncToPost)
                .build();

        CatCheckinVO vo = catCheckinService.checkin(dto);
        return Result.success(vo);
    }

    @Operation(summary = "上传猫咪打卡照片")
    @PostMapping("/upload")
    public Result<String> uploadPhotos(@RequestParam MultipartFile photo) {
        String objectName = "checkin/" + System.currentTimeMillis() + ".jpg";
        String photoURL = ossService.uploadFile(photo, objectName);
        return Result.success(photoURL);
    }

    @Operation(summary = "获取打卡时间轴/相册")
    @GetMapping("/timeline")
    public Result<List<CatCheckinVO>> getTimeline(
            @RequestParam(required = false) Long catProfileId) {
        List<CatCheckinVO> timeline = catCheckinService.getTimeline(catProfileId);
        return Result.success(timeline);
    }

    @Operation(summary = "获取打卡日历视图")
    @GetMapping("/calendar")
    public Result<CatCheckinCalendarVO> getCalendar(
            @RequestParam(required = false) Long catProfileId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        YearMonth yearMonth;
        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        } else {
            yearMonth = YearMonth.now();
        }

        CatCheckinCalendarVO calendar = catCheckinService.getCalendar(catProfileId, yearMonth);
        return Result.success(calendar);
    }

    @Operation(summary = "获取连续打卡天数")
    @GetMapping("/consecutive")
    public Result<Integer> getConsecutiveDays(
            @RequestParam(required = false) Long catProfileId) {
        Integer consecutiveDays = catCheckinService.getConsecutiveDays(catProfileId);
        return Result.success(consecutiveDays);
    }

    @Operation(summary = "同步打卡到社区")
    @PostMapping("/sync/{checkinId}")
    public Result<?> syncToPost(@PathVariable Long checkinId) {
        catCheckinService.syncToPost(checkinId);
        return Result.success("同步成功");
    }

    @Operation(summary = "获取打卡详情")
    @GetMapping("/{checkinId}")
    public Result<CatCheckinVO> getCheckinDetail(@PathVariable Long checkinId) {
        CatCheckinVO vo = catCheckinService.getCheckinById(checkinId);
        return Result.success(vo);
    }
}