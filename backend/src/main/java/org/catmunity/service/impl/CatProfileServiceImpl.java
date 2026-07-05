package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.exception.BusinessNotFoundException;
import org.catmunity.mapper.CatProfileMapper;
import org.catmunity.model.dto.CatProfileDTO;
import org.catmunity.model.dto.CatProfileQueryDTO;
import org.catmunity.model.entity.CatHealthEvent;
import org.catmunity.model.entity.CatProfile;
import org.catmunity.model.entity.User;
import org.catmunity.model.vo.CatHealthEventVO;
import org.catmunity.model.vo.CatProfileExcelVO;
import org.catmunity.model.vo.CatProfileVO;
import org.catmunity.result.PageResult;
import org.catmunity.service.CatHealthEventService;
import org.catmunity.service.CatProfileService;
import org.catmunity.service.OSSService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatProfileServiceImpl extends ServiceImpl<CatProfileMapper, CatProfile> implements CatProfileService {

    private final OSSService ossService;
    private final CatHealthEventService catHealthEventService;

    @Override
    public CatProfileVO createCatProfile(CatProfileDTO catProfileDTO) {
        CatProfile catProfile = new CatProfile();
        BeanUtils.copyProperties(catProfileDTO, catProfile);
        if (catProfile.getPhotoUrls() == null) {
            catProfile.setPhotoUrls(new ArrayList<>());
        }
        catProfile.setCreatedAt(LocalDateTime.now());
        catProfile.setUpdatedAt(LocalDateTime.now());
        save(catProfile);
        log.info("创建猫咪档案成功：id={}, name={}", catProfile.getId(), catProfile.getName());
        return toVO(catProfile);
    }

    @Override
    public CatProfileVO getCatProfileById(Long id) {
        CatProfile catProfile = getById(id);
        if (catProfile == null) {
            throw new BusinessNotFoundException("猫咪档案不存在");
        }
        return toVO(catProfile);
    }

    @Override
    public PageResult<CatProfileVO> getCatProfilePage(Integer pageNum, Integer pageSize) {
        Page<CatProfile> page = new Page<>(pageNum, pageSize);
        page = page(page, new LambdaQueryWrapper<CatProfile>().orderByDesc(CatProfile::getCreatedAt));
        List<CatProfileVO> voList = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(page.getTotal(), page.getPages(), pageNum, pageSize, voList);
    }

    @Override
    public List<CatProfileVO> getAllCatProfiles() {
        List<CatProfile> list = list(new LambdaQueryWrapper<CatProfile>().orderByDesc(CatProfile::getCreatedAt));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public CatProfileVO updateCatProfile(CatProfileDTO catProfileDTO) {
        if (catProfileDTO.getId() == null) {
            throw new BusinessNotFoundException("猫咪档案ID不能为空");
        }
        CatProfile existing = getById(catProfileDTO.getId());
        if (existing == null) {
            throw new BusinessNotFoundException("猫咪档案不存在");
        }
        BeanUtils.copyProperties(catProfileDTO, existing, "id", "createdAt");
        updateById(existing);
        log.info("更新猫咪档案成功：id={}", existing.getId());
        return toVO(existing);
    }

    @Override
    public void deleteCatProfile(Long id) {
        CatProfile catProfile = getById(id);
        if (catProfile == null) {
            throw new BusinessNotFoundException("猫咪档案不存在");
        }
        if (catProfile.getPhotoUrls() != null) {
            for (String photoUrl : catProfile.getPhotoUrls()) {
                deleteOSSFile(photoUrl);
            }
        }
        catHealthEventService.remove(new LambdaQueryWrapper<CatHealthEvent>().eq(CatHealthEvent::getCatProfileId, id));
        removeById(id);
        log.info("删除猫咪档案成功：id={}", id);
    }

    @Override
    public CatProfileVO uploadCatPhotos(Long id, MultipartFile[] files) {
        CatProfile catProfile = getById(id);
        if (catProfile == null) {
            throw new BusinessNotFoundException("猫咪档案不存在");
        }
        List<String> photoUrls = catProfile.getPhotoUrls();
        if (photoUrls == null) {
            photoUrls = new ArrayList<>();
        }
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            String objectName = buildObjectName(id, file.getOriginalFilename());
            String photoUrl = ossService.uploadFile(file, objectName);
            photoUrls.add(photoUrl);
            log.info("猫咪图片上传成功：catId={}, url={}", id, photoUrl);
        }
        catProfile.setPhotoUrls(photoUrls);
        updateById(catProfile);
        return toVO(catProfile);
    }


    @Override
    public List<String> uploadPhotos(MultipartFile[] files) {
        List<String> photoUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            String objectName = buildObjectName(0L, file.getOriginalFilename());
            String photoUrl = ossService.uploadFile(file, objectName);
            photoUrls.add(photoUrl);
            log.info("猫咪图片上传成功： url={}", photoUrl);
        }
        return photoUrls;
    }

    @Override
    public CatProfileVO deleteCatPhoto(Long id, String photoUrl) {
        CatProfile catProfile = getById(id);
        if (catProfile == null) {
            throw new BusinessNotFoundException("猫咪档案不存在");
        }
        List<String> photoUrls = catProfile.getPhotoUrls();
        if (photoUrls != null) {
            photoUrls.remove(photoUrl);
            deleteOSSFile(photoUrl);
            catProfile.setPhotoUrls(photoUrls);
            updateById(catProfile);
            log.info("删除猫咪图片成功：catId={}, url={}", id, photoUrl);
        }
        return toVO(catProfile);
    }

    @Override
    public PageResult<CatProfileVO> getAdminCatProfilePage(CatProfileQueryDTO queryDTO, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<CatProfile> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getName() != null && !queryDTO.getName().isEmpty()) {
            wrapper.like(CatProfile::getName, queryDTO.getName());
        }
        if (queryDTO.getBreed() != null && !queryDTO.getBreed().isEmpty()) {
            wrapper.like(CatProfile::getBreed, queryDTO.getBreed());
        }
        if (queryDTO.getGender() != null) {
            wrapper.eq(CatProfile::getGender, queryDTO.getGender());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(CatProfile::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getCreatedAtStart() != null) {
            wrapper.ge(CatProfile::getCreatedAt, queryDTO.getCreatedAtStart());
        }
        if (queryDTO.getCreatedAtEnd() != null) {
            wrapper.le(CatProfile::getCreatedAt, queryDTO.getCreatedAtEnd());
        }

        wrapper.orderByDesc(CatProfile::getCreatedAt);

        Page<CatProfile> page = new Page<>(pageNum, pageSize);
        page = page(page, wrapper);

        List<CatProfileVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), page.getPages(), pageNum, pageSize, voList);
    }

    @Override
    public List<CatProfileExcelVO> exportCatProfiles(CatProfileQueryDTO queryDTO) {
        LambdaQueryWrapper<CatProfile> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getName() != null && !queryDTO.getName().isEmpty()) {
            wrapper.like(CatProfile::getName, queryDTO.getName());
        }
        if (queryDTO.getBreed() != null && !queryDTO.getBreed().isEmpty()) {
            wrapper.like(CatProfile::getBreed, queryDTO.getBreed());
        }
        if (queryDTO.getGender() != null) {
            wrapper.eq(CatProfile::getGender, queryDTO.getGender());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(CatProfile::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getCreatedAtStart() != null) {
            wrapper.ge(CatProfile::getCreatedAt, queryDTO.getCreatedAtStart());
        }
        if (queryDTO.getCreatedAtEnd() != null) {
            wrapper.le(CatProfile::getCreatedAt, queryDTO.getCreatedAtEnd());
        }

        wrapper.orderByDesc(CatProfile::getCreatedAt);

        List<CatProfile> records = list(wrapper);

        return records.stream()
                .map(cat -> CatProfileExcelVO.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .breed(cat.getBreed())
                        .genderStr(cat.getGender() != null ? (cat.getGender() == 0 ? "母猫" : "公猫") : "未知")
                        .birthday(cat.getBirthday())
                        .weight(cat.getWeight())
                        .statusStr(getStatusStr(cat.getStatus()))
                        .createdAt(cat.getCreatedAt())
                        .updatedAt(cat.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private String getStatusStr(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "正常";
            case 1: return "已领养";
            case 2: return "其他";
            default: return "未知";
        }
    }

    private String buildObjectName(Long catId, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "cat-profile/" + catId + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private void deleteOSSFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            String objectName = extractObjectName(fileUrl);
            if (objectName != null) {
                ossService.deleteFile(objectName);
            }
        } catch (Exception e) {
            log.warn("删除OSS文件失败：url={}, error={}", fileUrl, e.getMessage());
        }
    }

    private String extractObjectName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        int index = fileUrl.indexOf("/cat-profile/");
        if (index > 0) {
            return fileUrl.substring(index + 1);
        }
        return null;
    }

    private CatProfileVO toVO(CatProfile catProfile) {
        CatProfileVO vo = new CatProfileVO();
        BeanUtils.copyProperties(catProfile, vo);
        List<CatHealthEventVO> events = catHealthEventService.getTimelineByCatProfileId(catProfile.getId());
        vo.setHealthEvents(events);
        return vo;
    }
}