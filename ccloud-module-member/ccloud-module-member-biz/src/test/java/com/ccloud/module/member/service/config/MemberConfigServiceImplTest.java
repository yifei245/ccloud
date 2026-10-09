package com.ccloud.module.member.service.config;

import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.member.controller.admin.config.vo.MemberConfigSaveReqVO;
import com.ccloud.module.member.dal.dataobject.config.MemberConfigDO;
import com.ccloud.module.member.dal.mysql.config.MemberConfigMapper;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(MemberConfigServiceImpl.class)
class MemberConfigServiceImplTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberConfigServiceImpl configService;
    @Resource
    private MemberConfigMapper configMapper;

    @Test
    void saveConfig_不存在时新增_存在时修改_每个租户一条() {
        assertThat(configService.getConfig()).isNull();

        configService.saveConfig(save(true, 1, 10000));
        configService.saveConfig(save(false, 2, null));

        assertThat(configMapper.selectCount()).isEqualTo(1);
        MemberConfigDO config = configService.getConfig();
        assertThat(config.getPointTradeDeductEnable()).isFalse();
        assertThat(config.getPointTradeDeductUnitPrice()).isEqualTo(2);
        assertThat(config.getPointTradeGivePoint()).isEqualTo(5L);
        assertThat(config.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void saveConfig_可清空可选项() {
        configService.saveConfig(save(true, 1, 10000));
        configService.saveConfig(save(true, 1, null));
        assertThat(configService.getConfig().getPointTradeDeductMaxPrice()).isNull();
    }

    @Test
    void getConfig_租户隔离_各租户各自一条() {
        configService.saveConfig(save(true, 1, 10000));
        assertThat(TenantUtils.execute(TENANT_ID + 1000, () -> configService.getConfig())).isNull();

        TenantUtils.execute(TENANT_ID + 1000, () -> configService.saveConfig(save(false, 3, null)));

        assertThat(configService.getConfig().getPointTradeDeductUnitPrice()).isEqualTo(1);
        assertThat(TenantUtils.execute(TENANT_ID + 1000, () -> configService.getConfig()).getPointTradeDeductUnitPrice())
                .isEqualTo(3);
    }

    private static MemberConfigSaveReqVO save(boolean enable, int unitPrice, Integer maxPrice) {
        MemberConfigSaveReqVO reqVO = new MemberConfigSaveReqVO();
        reqVO.setPointTradeDeductEnable(enable);
        reqVO.setPointTradeDeductUnitPrice(unitPrice);
        reqVO.setPointTradeDeductMaxPrice(maxPrice);
        reqVO.setPointTradeGivePoint(5L);
        return reqVO;
    }
}
