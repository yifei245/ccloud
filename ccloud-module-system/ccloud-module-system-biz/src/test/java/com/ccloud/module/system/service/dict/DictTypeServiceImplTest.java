package com.ccloud.module.system.service.dict;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypePageReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypeSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dict.DictDataDO;
import com.ccloud.module.system.dal.dataobject.dict.DictTypeDO;
import com.ccloud.module.system.dal.mysql.dict.DictDataMapper;
import com.ccloud.module.system.dal.mysql.dict.DictTypeMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import({DictTypeServiceImpl.class, DictDataServiceImpl.class, CacheTestConfiguration.class})
class DictTypeServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private DictTypeService dictTypeService;
    @Resource
    private DictTypeMapper dictTypeMapper;
    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void createDictType_成功() {
        Long id = dictTypeService.createDictType(newReqVO("性别", "demo_sex"));
        DictTypeDO type = dictTypeMapper.selectById(id);
        assertThat(type.getType()).isEqualTo("demo_sex");
        assertThat(type.getName()).isEqualTo("性别");
    }

    @Test
    void createDictType_类型重复() {
        dictTypeService.createDictType(newReqVO("性别", "demo_sex"));
        assertServiceException(() -> dictTypeService.createDictType(newReqVO("性别2", "demo_sex")), DICT_TYPE_TYPE_DUPLICATE);
    }

    @Test
    void createDictType_名称重复() {
        dictTypeService.createDictType(newReqVO("性别", "demo_sex"));
        assertServiceException(() -> dictTypeService.createDictType(newReqVO("性别", "demo_sex2")), DICT_TYPE_NAME_DUPLICATE);
    }

    @Test
    void updateDictType_不允许修改类型标识_否则其下字典数据成为孤儿() {
        Long id = dictTypeService.createDictType(newReqVO("状态", "demo_status"));
        DictTypeSaveReqVO reqVO = newReqVO("状态-改", "demo_status_changed");
        reqVO.setId(id);
        dictTypeService.updateDictType(reqVO);

        DictTypeDO type = dictTypeMapper.selectById(id);
        assertThat(type.getName()).isEqualTo("状态-改");
        assertThat(type.getType()).isEqualTo("demo_status");
    }

    @Test
    void updateDictType_不存在() {
        DictTypeSaveReqVO reqVO = newReqVO("x", "x");
        reqVO.setId(-1L);
        assertServiceException(() -> dictTypeService.updateDictType(reqVO), DICT_TYPE_NOT_EXISTS);
    }

    @Test
    void deleteDictType_成功_同一类型可反复删除与重建() {
        Long id = dictTypeService.createDictType(newReqVO("临时", "demo_tmp"));
        dictTypeService.deleteDictType(id);
        assertThat(dictTypeMapper.selectById(id)).isNull();

        Long again = dictTypeService.createDictType(newReqVO("临时", "demo_tmp"));
        dictTypeService.deleteDictType(again);
        assertThat(dictTypeService.createDictType(newReqVO("临时", "demo_tmp"))).isNotNull();
    }

    @Test
    void deleteDictType_有字典数据时失败() {
        Long id = dictTypeService.createDictType(newReqVO("有数据", "demo_has"));
        dictDataMapper.insert(new DictDataDO().setDictType("demo_has").setLabel("a").setValue("1").setSort(0).setStatus(0));
        assertServiceException(() -> dictTypeService.deleteDictType(id), DICT_TYPE_HAS_CHILDREN);
    }

    @Test
    void deleteDictType_不存在() {
        assertServiceException(() -> dictTypeService.deleteDictType(-1L), DICT_TYPE_NOT_EXISTS);
    }

    @Test
    void deleteDictTypeList() {
        Long a = dictTypeService.createDictType(newReqVO("甲", "demo_a"));
        Long b = dictTypeService.createDictType(newReqVO("乙", "demo_b"));
        dictTypeService.deleteDictTypeList(List.of(a, b));
        assertThat(dictTypeMapper.selectCount()).isZero();
    }

    @Test
    void 查询() {
        Long id = dictTypeService.createDictType(newReqVO("用户性别", "demo_user_sex"));
        Long disabled = dictTypeService.createDictType(newReqVO("用户状态", "demo_user_status"));
        dictTypeMapper.updateById(new DictTypeDO().setId(disabled).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        dictTypeService.createDictType(newReqVO("订单状态", "demo_order_status"));

        assertThat(dictTypeService.getDictType(id).getType()).isEqualTo("demo_user_sex");
        assertThat(dictTypeService.getDictType("demo_user_sex").getId()).isEqualTo(id);
        assertThat(dictTypeService.getDictTypeList()).hasSize(3);

        DictTypePageReqVO reqVO = new DictTypePageReqVO();
        reqVO.setType("demo_user");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<DictTypeDO> page = dictTypeService.getDictTypePage(reqVO);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).extracting(DictTypeDO::getType).containsExactly("demo_user_sex");
    }

    private static DictTypeSaveReqVO newReqVO(String name, String type) {
        DictTypeSaveReqVO reqVO = new DictTypeSaveReqVO();
        reqVO.setName(name);
        reqVO.setType(type);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setRemark("");
        return reqVO;
    }
}
