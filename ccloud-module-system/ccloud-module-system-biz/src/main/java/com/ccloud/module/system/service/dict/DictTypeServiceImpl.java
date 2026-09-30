package com.ccloud.module.system.service.dict;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypePageReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypeSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dict.DictTypeDO;
import com.ccloud.module.system.dal.mysql.dict.DictTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class DictTypeServiceImpl implements DictTypeService {

    @Resource
    private DictTypeMapper dictTypeMapper;
    @Resource
    @Lazy
    private DictDataService dictDataService;

    @Override
    public Long createDictType(DictTypeSaveReqVO createReqVO) {
        validateDictTypeNameUnique(null, createReqVO.getName());
        validateDictTypeUnique(null, createReqVO.getType());

        DictTypeDO dictType = BeanUtils.toBean(createReqVO, DictTypeDO.class);
        dictTypeMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public void updateDictType(DictTypeSaveReqVO updateReqVO) {
        validateDictTypeExists(updateReqVO.getId());
        validateDictTypeNameUnique(updateReqVO.getId(), updateReqVO.getName());

        DictTypeDO updateObj = BeanUtils.toBean(updateReqVO, DictTypeDO.class);
        updateObj.setType(null);
        dictTypeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictType(Long id) {
        DictTypeDO dictType = validateDictTypeExists(id);
        if (dictDataService.getDictDataCountByDictType(dictType.getType()) > 0) {
            throw exception(DICT_TYPE_HAS_CHILDREN);
        }
        dictTypeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictTypeList(List<Long> ids) {
        ids.forEach(this::deleteDictType);
    }

    private DictTypeDO validateDictTypeExists(Long id) {
        DictTypeDO dictType = id != null ? dictTypeMapper.selectById(id) : null;
        if (dictType == null) {
            throw exception(DICT_TYPE_NOT_EXISTS);
        }
        return dictType;
    }

    private void validateDictTypeUnique(Long id, String type) {
        DictTypeDO dictType = dictTypeMapper.selectByType(type);
        if (dictType != null && !dictType.getId().equals(id)) {
            throw exception(DICT_TYPE_TYPE_DUPLICATE);
        }
    }

    private void validateDictTypeNameUnique(Long id, String name) {
        DictTypeDO dictType = dictTypeMapper.selectByName(name);
        if (dictType != null && !dictType.getId().equals(id)) {
            throw exception(DICT_TYPE_NAME_DUPLICATE);
        }
    }

    @Override
    public DictTypeDO getDictType(Long id) {
        return dictTypeMapper.selectById(id);
    }

    @Override
    public DictTypeDO getDictType(String type) {
        return dictTypeMapper.selectByType(type);
    }

    @Override
    public List<DictTypeDO> getDictTypeList() {
        return dictTypeMapper.selectList();
    }

    @Override
    public PageResult<DictTypeDO> getDictTypePage(DictTypePageReqVO reqVO) {
        return dictTypeMapper.selectPage(reqVO);
    }
}
