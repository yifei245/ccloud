package com.ccloud.module.system.service.dict;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataPageReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dict.DictDataDO;
import com.ccloud.module.system.dal.dataobject.dict.DictTypeDO;
import com.ccloud.module.system.dal.mysql.dict.DictDataMapper;
import com.ccloud.module.system.dal.mysql.dict.DictTypeMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import({DictDataServiceImpl.class, DictTypeServiceImpl.class, CacheTestConfiguration.class})
class DictDataServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private DictDataService dictDataService;
    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private DictTypeMapper dictTypeMapper;

    @BeforeEach
    void seedTypes() {
        insertType("demo_sex", CommonStatusEnum.ENABLE);
        insertType("demo_off", CommonStatusEnum.DISABLE);
    }

    // ========== 增删改 ==========

    @Test
    void createDictData_成功() {
        Long id = dictDataService.createDictData(newReqVO("demo_sex", "男", "1"));
        DictDataDO data = dictDataMapper.selectById(id);
        assertThat(data.getLabel()).isEqualTo("男");
        assertThat(data.getValue()).isEqualTo("1");
    }

    @Test
    void createDictData_同类型下值重复失败_不同类型允许同值() {
        insertType("demo_other", CommonStatusEnum.ENABLE);
        dictDataService.createDictData(newReqVO("demo_sex", "男", "1"));

        assertServiceException(() -> dictDataService.createDictData(newReqVO("demo_sex", "未知", "1")),
                DICT_DATA_VALUE_DUPLICATE);
        assertThat(dictDataService.createDictData(newReqVO("demo_other", "是", "1"))).isNotNull();
    }

    @Test
    void createDictData_字典类型不存在或已停用() {
        assertServiceException(() -> dictDataService.createDictData(newReqVO("demo_none", "a", "1")), DICT_TYPE_NOT_EXISTS);
        assertServiceException(() -> dictDataService.createDictData(newReqVO("demo_off", "a", "1")), DICT_TYPE_NOT_ENABLE);
    }

    @Test
    void updateDictData_成功_与自身相同的值允许() {
        Long id = dictDataService.createDictData(newReqVO("demo_sex", "男", "1"));
        DictDataSaveReqVO reqVO = newReqVO("demo_sex", "男性", "1");
        reqVO.setId(id);
        dictDataService.updateDictData(reqVO);
        assertThat(dictDataMapper.selectById(id).getLabel()).isEqualTo("男性");
    }

    @Test
    void updateDictData_不存在() {
        DictDataSaveReqVO reqVO = newReqVO("demo_sex", "x", "9");
        reqVO.setId(-1L);
        assertServiceException(() -> dictDataService.updateDictData(reqVO), DICT_DATA_NOT_EXISTS);
    }

    @Test
    void deleteDictData_成功与不存在() {
        Long id = dictDataService.createDictData(newReqVO("demo_sex", "男", "1"));
        dictDataService.deleteDictData(id);
        assertThat(dictDataMapper.selectById(id)).isNull();
        assertServiceException(() -> dictDataService.deleteDictData(-1L), DICT_DATA_NOT_EXISTS);
    }

    @Test
    void deleteDictDataList() {
        Long a = dictDataService.createDictData(newReqVO("demo_sex", "男", "1"));
        Long b = dictDataService.createDictData(newReqVO("demo_sex", "女", "2"));
        dictDataService.deleteDictDataList(List.of(a, b));
        assertThat(dictDataMapper.selectCount()).isZero();
    }

    // ========== 标签与值互转 ==========

    @Test
    void 标签与值互转正确_未命中返回null() {
        dictDataService.createDictData(newReqVO("demo_sex", "男", "1"));
        dictDataService.createDictData(newReqVO("demo_sex", "女", "2"));

        assertThat(dictDataService.getDictDataLabel("demo_sex", "2")).isEqualTo("女");
        assertThat(dictDataService.parseDictDataValue("demo_sex", "男")).isEqualTo("1");
        assertThat(dictDataService.getDictData("demo_sex", "1").getLabel()).isEqualTo("男");
        assertThat(dictDataService.parseDictData("demo_sex", "女").getValue()).isEqualTo("2");

        assertThat(dictDataService.getDictDataLabel("demo_sex", "9")).isNull();
        assertThat(dictDataService.parseDictDataValue("demo_sex", "不存在")).isNull();
        assertThat(dictDataService.getDictDataLabel("demo_none", "1")).isNull();
    }

    @Test
    void 修改后缓存失效_翻译结果立即更新() {
        Long id = dictDataService.createDictData(newReqVO("demo_sex", "男", "1"));
        assertThat(dictDataService.getDictDataLabel("demo_sex", "1")).isEqualTo("男");

        DictDataSaveReqVO reqVO = newReqVO("demo_sex", "男性", "1");
        reqVO.setId(id);
        dictDataService.updateDictData(reqVO);

        assertThat(dictDataService.getDictDataLabel("demo_sex", "1")).isEqualTo("男性");
    }

    @Test
    void 字典为全局数据_不同租户读到相同结果() {
        dictDataService.createDictData(newReqVO("demo_sex", "男", "1"));
        String otherTenant = TenantUtils.execute(99L, () -> dictDataService.getDictDataLabel("demo_sex", "1"));
        assertThat(otherTenant).isEqualTo("男");
    }

    // ========== 查询与校验 ==========

    @Test
    void getDictDataList_按状态与类型_按排序() {
        insertData("demo_sex", "女", "2", 2, CommonStatusEnum.ENABLE);
        insertData("demo_sex", "男", "1", 1, CommonStatusEnum.ENABLE);
        insertData("demo_sex", "未知", "0", 0, CommonStatusEnum.DISABLE);

        assertThat(dictDataService.getDictDataList(CommonStatusEnum.ENABLE.getStatus(), "demo_sex"))
                .extracting(DictDataDO::getLabel).containsExactly("男", "女");
        assertThat(dictDataService.getDictDataListByDictType("demo_sex")).hasSize(3);
        assertThat(dictDataService.getDictDataCountByDictType("demo_sex")).isEqualTo(3);
    }

    @Test
    void getDictDataPage_按条件过滤() {
        insertData("demo_sex", "男", "1", 1, CommonStatusEnum.ENABLE);
        insertData("demo_sex", "女", "2", 2, CommonStatusEnum.DISABLE);

        DictDataPageReqVO reqVO = new DictDataPageReqVO();
        reqVO.setDictType("demo_sex");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<DictDataDO> page = dictDataService.getDictDataPage(reqVO);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).extracting(DictDataDO::getLabel).containsExactly("男");
    }

    @Test
    void validateDictDataList_三个分支() {
        insertData("demo_sex", "男", "1", 1, CommonStatusEnum.ENABLE);
        insertData("demo_sex", "女", "2", 2, CommonStatusEnum.DISABLE);

        dictDataService.validateDictDataList("demo_sex", List.of("1"));
        dictDataService.validateDictDataList("demo_sex", List.of());
        assertServiceException(() -> dictDataService.validateDictDataList("demo_sex", List.of("9")), DICT_DATA_NOT_EXISTS);
        assertServiceException(() -> dictDataService.validateDictDataList("demo_sex", List.of("2")), DICT_DATA_NOT_ENABLE);
    }

    // ========== 工具 ==========

    private void insertType(String type, CommonStatusEnum status) {
        dictTypeMapper.insert(new DictTypeDO().setName(type).setType(type).setStatus(status.getStatus()));
    }

    private void insertData(String dictType, String label, String value, int sort, CommonStatusEnum status) {
        dictDataMapper.insert(new DictDataDO().setDictType(dictType).setLabel(label).setValue(value)
                .setSort(sort).setStatus(status.getStatus()));
    }

    private static DictDataSaveReqVO newReqVO(String dictType, String label, String value) {
        DictDataSaveReqVO reqVO = new DictDataSaveReqVO();
        reqVO.setDictType(dictType);
        reqVO.setLabel(label);
        reqVO.setValue(value);
        reqVO.setSort(1);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }
}
