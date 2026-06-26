package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.catmunity.mapper.DatasetFilesMapper;
import org.catmunity.model.dto.DatasetFilesPageDTO;
import org.catmunity.model.entity.DatasetFiles;
import org.catmunity.service.DataSetFilesService;
import org.springframework.stereotype.Service;

@Service
public class DataSetFilesServiceImpl extends ServiceImpl<DatasetFilesMapper, DatasetFiles> implements DataSetFilesService {

    @Override
    public IPage<DatasetFiles> getDatasetFilesPage(Integer current, Integer size) {
        current = current != null ? current : 1;
        size = size != null ? size : 10;

        Page<DatasetFiles> page = new Page<>(current, size);

        return this.page(page);
    }
}