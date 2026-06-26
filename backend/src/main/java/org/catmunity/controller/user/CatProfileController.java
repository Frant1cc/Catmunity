package org.catmunity.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.model.dto.CatProfileDTO;
import org.catmunity.model.vo.CatProfileVO;
import org.catmunity.result.PageResult;
import org.catmunity.result.Result;
import org.catmunity.service.CatProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "用户端-猫咪档案管理")
@RestController
@Slf4j
@RequestMapping("/catProfile")
public class CatProfileController {
    @Autowired
    private CatProfileService catProfileService;

    @Operation(summary = "根据ID获取猫咪档案")
    @GetMapping("/{id}")
    public Result<CatProfileVO> getCatProfileById(@PathVariable Long id) {
        CatProfileVO vo = catProfileService.getCatProfileById(id);
        return Result.success(vo);
    }

    @Operation(summary = "分页查询猫咪档案列表")
    @GetMapping("/list")
    public Result<PageResult<CatProfileVO>> getCatProfileList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<CatProfileVO> pageResult = catProfileService.getCatProfilePage(pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "获取所有猫咪档案")
    @GetMapping("/all")
    public Result<List<CatProfileVO>> getAllCatProfiles() {
        List<CatProfileVO> list = catProfileService.getAllCatProfiles();
        return Result.success(list);
    }
}