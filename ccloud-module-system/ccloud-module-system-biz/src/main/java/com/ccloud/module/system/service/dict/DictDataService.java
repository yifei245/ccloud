package com.ccloud.module.system.service.dict;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataPageReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dict.DictDataDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * 字典数据 Service
 */
public interface DictDataService {

    Long createDictData(@Valid DictDataSaveReqVO createReqVO);

    void updateDictData(@Valid DictDataSaveReqVO updateReqVO);

    void deleteDictData(Long id);

    void deleteDictDataList(List<Long> ids);

    DictDataDO getDictData(Long id);

    /**
     * @param status   为空时不按状态过滤
     * @param dictType 为空时不按类型过滤
     */
    List<DictDataDO> getDictDataList(Integer status, String dictType);

    PageResult<DictDataDO> getDictDataPage(DictDataPageReqVO reqVO);

    long getDictDataCountByDictType(String dictType);

    /**
     * @return 字典类型下的全部字典数据（含停用），按排序；带缓存
     */
    List<DictDataDO> getDictDataListByDictType(String dictType);

    /**
     * @return 按值查找，未命中返回 null
     */
    DictDataDO getDictData(String dictType, String value);

    /**
     * @return 按标签查找，未命中返回 null
     */
    DictDataDO parseDictData(String dictType, String label);

    /**
     * @return 值对应的标签，未命中返回 null
     */
    String getDictDataLabel(String dictType, String value);

    /**
     * @return 标签对应的值，未命中返回 null
     */
    String parseDictDataValue(String dictType, String label);

    /**
     * 校验字典值均存在且开启
     */
    void validateDictDataList(String dictType, Collection<String> values);
}
