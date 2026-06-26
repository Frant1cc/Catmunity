package org.catmunity.service;

import org.catmunity.model.dto.SubaccountCreateDTO;
import org.catmunity.model.dto.SubaccountQueryDTO;
import org.catmunity.model.dto.SubaccountUpdateDTO;
import org.catmunity.model.vo.SubaccountVO;
import org.catmunity.result.PageResult;
import org.catmunity.result.Result;

public interface SubaccountService {

    // 创建子账号
    Result<?> createSubaccount(SubaccountCreateDTO createDTO);

    // 查询子账号列表
    PageResult<SubaccountVO> querySubaccounts(Integer pageNum, Integer pageSize, SubaccountQueryDTO queryDTO);

    // 修改子账号
    SubaccountVO updateSubaccount(Integer subaccountId, SubaccountUpdateDTO updateDTO);

    // 删除子账号
    Integer deleteSubaccount(Integer subaccountId);

}
