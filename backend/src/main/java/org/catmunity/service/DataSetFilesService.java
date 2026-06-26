package org.catmunity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.DatasetFilesPageDTO;
import org.catmunity.model.entity.DatasetFiles;

public interface DataSetFilesService extends IService<DatasetFiles> {
    
    IPage<DatasetFiles> getDatasetFilesPage(Integer current, Integer size);
}