package com.ccloud.module.system.service.dict;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataPageReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dict.DictDataDO;
import com.ccloud.module.system.dal.dataobject.dict.DictTypeDO;
import com.ccloud.module.system.dal.mysql.dict.DictDataMapper;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.dal.redis.RedisKeyConstants.DICT_DATA_LIST;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class DictDataServiceImpl implements DictDataService {

    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private DictTypeService dictTypeService;
    /**
     * 自身代理：带缓存注解的方法在类内调用时必须经过代理才生效
     */
    @Resource
    @Lazy
    private DictDataService self;

    // ========== 增删改：修改可能变更所属类型，按整体失效处理 ==========

    @Override
    @CacheEvict(value = DICT_DATA_LIST, allEntries = true)
    public Long createDictData(DictDataSaveReqVO createReqVO) {
        validateDictTypeAvailable(createReqVO.getDictType());
        validateDictDataValueUnique(null, createReqVO.getDictType(), createReqVO.getValue());

        DictDataDO dictData = BeanUtils.toBean(createReqVO, DictDataDO.class);
        dictDataMapper.insert(dictData);
        return dictData.getId();
    }

    @Override
    @CacheEvict(value = DICT_DATA_LIST, allEntries = true)
    public void updateDictData(DictDataSaveReqVO updateReqVO) {
        validateDictDataExists(updateReqVO.getId());
        validateDictTypeAvailable(updateReqVO.getDictType());
        validateDictDataValueUnique(updateReqVO.getId(), updateReqVO.getDictType(), updateReqVO.getValue());

        dictDataMapper.updateById(BeanUtils.toBean(updateReqVO, DictDataDO.class));
    }

    @Override
    @CacheEvict(value = DICT_DATA_LIST, allEntries = true)
    public void deleteDictData(Long id) {
        validateDictDataExists(id);
        dictDataMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = DICT_DATA_LIST, allEntries = true)
    public void deleteDictDataList(List<Long> ids) {
        ids.forEach(this::validateDictDataExists);
        dictDataMapper.deleteByIds(ids);
    }

    private void validateDictDataExists(Long id) {
        if (id == null || dictDataMapper.selectById(id) == null) {
            throw exception(DICT_DATA_NOT_EXISTS);
        }
    }

    private void validateDictTypeAvailable(String type) {
        DictTypeDO dictType = dictTypeService.getDictType(type);
        if (dictType == null) {
            throw exception(DICT_TYPE_NOT_EXISTS);
        }
        if (!CommonStatusEnum.isEnable(dictType.getStatus())) {
            throw exception(DICT_TYPE_NOT_ENABLE);
        }
    }

    /**
     * 同一字典类型下值唯一
     */
    private void validateDictDataValueUnique(Long id, String dictType, String value) {
        DictDataDO dictData = dictDataMapper.selectByDictTypeAndValue(dictType, value);
        if (dictData != null && !dictData.getId().equals(id)) {
            throw exception(DICT_DATA_VALUE_DUPLICATE);
        }
    }

    // ========== 查询 ==========

    @Override
    public DictDataDO getDictData(Long id) {
        return dictDataMapper.selectById(id);
    }

    @Override
    public List<DictDataDO> getDictDataList(Integer status, String dictType) {
        return dictDataMapper.selectListByStatusAndDictType(status, dictType);
    }

    @Override
    public PageResult<DictDataDO> getDictDataPage(DictDataPageReqVO reqVO) {
        return dictDataMapper.selectPage(reqVO);
    }

    @Override
    public long getDictDataCountByDictType(String dictType) {
        return dictDataMapper.selectCountByDictType(dictType);
    }

    @Override
    @Cacheable(value = DICT_DATA_LIST, key = "#dictType")
    public List<DictDataDO> getDictDataListByDictType(String dictType) {
        return dictDataMapper.selectListByStatusAndDictType(null, dictType);
    }

    @Override
    public DictDataDO getDictData(String dictType, String value) {
        return CollectionUtils.findFirst(self.getDictDataListByDictType(dictType),
                data -> data.getValue().equals(value));
    }

    @Override
    public DictDataDO parseDictData(String dictType, String label) {
        return CollectionUtils.findFirst(self.getDictDataListByDictType(dictType),
                data -> data.getLabel().equals(label));
    }

    @Override
    public String getDictDataLabel(String dictType, String value) {
        DictDataDO data = getDictData(dictType, value);
        return data != null ? data.getLabel() : null;
    }

    @Override
    public String parseDictDataValue(String dictType, String label) {
        DictDataDO data = parseDictData(dictType, label);
        return data != null ? data.getValue() : null;
    }

    @Override
    public void validateDictDataList(String dictType, Collection<String> values) {
        if (CollUtil.isEmpty(values)) {
            return;
        }
        Map<String, DictDataDO> dataMap = CollectionUtils.convertMap(
                self.getDictDataListByDictType(dictType), DictDataDO::getValue);
        for (String value : values) {
            DictDataDO data = dataMap.get(value);
            if (data == null) {
                throw exception(DICT_DATA_NOT_EXISTS);
            }
            if (!CommonStatusEnum.isEnable(data.getStatus())) {
                throw exception(DICT_DATA_NOT_ENABLE, data.getLabel());
            }
        }
    }
}
