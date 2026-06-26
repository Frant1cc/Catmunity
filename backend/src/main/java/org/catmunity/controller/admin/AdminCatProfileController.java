package org.catmunity.controller.admin;

import com.alibaba.excel.EasyExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.model.dto.CatProfileDTO;
import org.catmunity.model.dto.CatProfileQueryDTO;
import org.catmunity.model.vo.CatProfileExcelVO;
import org.catmunity.model.vo.CatProfileVO;
import org.catmunity.result.PageResult;
import org.catmunity.result.Result;
import org.catmunity.service.CatProfileService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "管理端-猫咪档案管理")
@RestController
@RequestMapping("/admin/catProfile")
@RequiredArgsConstructor
@Slf4j
public class AdminCatProfileController {

    private final CatProfileService catProfileService;

    @Operation(summary = "创建猫咪档案")
    @PostMapping
    public Result<CatProfileVO> createCatProfile(@RequestBody CatProfileDTO catProfileDTO) {
        CatProfileVO vo = catProfileService.createCatProfile(catProfileDTO);
        return Result.success(vo);
    }

    @Operation(summary = "更新猫咪档案")
    @PutMapping
    public Result<CatProfileVO> updateCatProfile(@RequestBody CatProfileDTO catProfileDTO) {
        CatProfileVO vo = catProfileService.updateCatProfile(catProfileDTO);
        return Result.success(vo);
    }

    @Operation(summary = "分页查询猫咪档案列表（管理端）")
    @GetMapping("/list")
    public Result<PageResult<CatProfileVO>> getCatProfilePage(
            @ModelAttribute CatProfileQueryDTO queryDTO,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<CatProfileVO> pageResult = catProfileService.getAdminCatProfilePage(queryDTO, pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "查看猫咪档案详情（管理端）")
    @GetMapping("/{id}")
    public Result<CatProfileVO> getCatProfileDetail(@PathVariable Long id) {
        CatProfileVO vo = catProfileService.getCatProfileById(id);
        return Result.success(vo);
    }

    @Operation(summary = "删除猫咪档案（管理端）")
    @DeleteMapping("/{id}")
    public Result<?> deleteCatProfile(@PathVariable Long id) {
        catProfileService.deleteCatProfile(id);
        return Result.success("档案删除成功");
    }

    @Operation(summary = "上传指定ID的猫咪图片")
    @PostMapping("/{id}/photos")
    public Result<CatProfileVO> uploadCatPhotos(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files) {
        CatProfileVO vo = catProfileService.uploadCatPhotos(id, files);
        return Result.success(vo);
    }

    @Operation(summary = "上传猫咪图片")
    @PostMapping("/photos")
    public Result<List<String>> uploadPhotos(@RequestParam("files") MultipartFile[] files) {
        List<String> urls = catProfileService.uploadPhotos(files);
        return Result.success(urls);
    }

    @Operation(summary = "删除猫咪图片")
    @DeleteMapping("/{id}/photos")
    public Result<CatProfileVO> deleteCatPhoto(
            @PathVariable Long id,
            @RequestParam String photoUrl) {
        CatProfileVO vo = catProfileService.deleteCatPhoto(id, photoUrl);
        return Result.success(vo);
    }

    @Operation(summary = "导出猫咪档案Excel（管理端）")
    @GetMapping("/export")
    public void exportCatProfiles(
            @ModelAttribute CatProfileQueryDTO queryDTO,
            HttpServletResponse response) throws IOException {
        List<CatProfileExcelVO> exportList = catProfileService.exportCatProfiles(queryDTO);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("猫咪档案列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), CatProfileExcelVO.class)
                .sheet("猫咪档案")
                .doWrite(exportList);
    }
}