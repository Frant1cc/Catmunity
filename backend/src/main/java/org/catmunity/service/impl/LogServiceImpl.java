package org.catmunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.catmunity.mapper.LogMapper;
import org.catmunity.model.entity.Log;
import org.catmunity.service.LogService;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {
}