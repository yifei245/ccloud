package com.ccloud.module.member.service.config;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.config.vo.MemberConfigSaveReqVO;
import com.ccloud.module.member.dal.dataobject.config.MemberConfigDO;
import com.ccloud.module.member.dal.mysql.config.MemberConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class MemberConfigServiceImpl implements MemberConfigService {

    @Resource
    private MemberConfigMapper configMapper;

    @Override
    public void saveConfig(MemberConfigSaveReqVO reqVO) {
        MemberConfigDO config = configMapper.selectCurrent();
        if (config == null) {
            configMapper.insert(BeanUtils.toBean(reqVO, MemberConfigDO.class));
            return;
        }
        // 可选项允许清空，用 UpdateWrapper 显式写入 null
        configMapper.update(null, new LambdaUpdateWrapper<MemberConfigDO>()
                .eq(MemberConfigDO::getId, config.getId())
                .set(MemberConfigDO::getPointTradeDeductEnable, reqVO.getPointTradeDeductEnable())
                .set(MemberConfigDO::getPointTradeDeductUnitPrice, reqVO.getPointTradeDeductUnitPrice())
                .set(MemberConfigDO::getPointTradeDeductMaxPrice, reqVO.getPointTradeDeductMaxPrice())
                .set(MemberConfigDO::getPointTradeGivePoint, reqVO.getPointTradeGivePoint()));
    }

    @Override
    public MemberConfigDO getConfig() {
        return configMapper.selectCurrent();
    }
}
