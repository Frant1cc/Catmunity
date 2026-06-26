package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.CatProfileDTO;
import org.catmunity.model.dto.CatProfileQueryDTO;
import org.catmunity.model.entity.CatProfile;
import org.catmunity.model.vo.CatProfileExcelVO;
import org.catmunity.model.vo.CatProfileVO;
import org.catmunity.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CatProfileService extends IService<CatProfile> {

    CatProfileVO createCatProfile(CatProfileDTO catProfileDTO);

    CatProfileVO getCatProfileById(Long id);

    PageResult<CatProfileVO> getCatProfilePage(Integer pageNum, Integer pageSize);

    List<CatProfileVO> getAllCatProfiles();

    CatProfileVO updateCatProfile(CatProfileDTO catProfileDTO);

    void deleteCatProfile(Long id);

    CatProfileVO uploadCatPhotos(Long id, MultipartFile[] files);

    CatProfileVO deleteCatPhoto(Long id, String photoUrl);

    PageResult<CatProfileVO> getAdminCatProfilePage(CatProfileQueryDTO queryDTO, Integer pageNum, Integer pageSize);

    List<CatProfileExcelVO> exportCatProfiles(CatProfileQueryDTO queryDTO);

    List<String> uploadPhotos(MultipartFile[] files);
}