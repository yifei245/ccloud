package com.ccloud.module.system.service.dict;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypePageReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypeSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dict.DictTypeDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 字典类型 Service
 */
public interface DictTypeService {

    Long createDictType(@Valid DictTypeSaveReqVO createReqVO);

    /**
     * 类型标识创建后不可修改：字典数据按标识关联，修改会使原有数据失联
     */
    void updateDictType(@Valid DictTypeSaveReqVO updateReqVO);

    /**
     * 存在字典数据时拒绝
     */
    void deleteDictType(Long id);

    void deleteDictTypeList(List<Long> ids);

    DictTypeDO getDictType(Long id);

    DictTypeDO getDictType(String type);

    List<DictTypeDO> getDictTypeList();

    PageResult<DictTypeDO> getDictTypePage(DictTypePageReqVO reqVO);
}
