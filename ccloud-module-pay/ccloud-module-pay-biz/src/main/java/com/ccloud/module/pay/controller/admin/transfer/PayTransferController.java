package com.ccloud.module.pay.controller.admin.transfer;

import cn.hutool.core.util.IdUtil;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.ccloud.module.pay.controller.admin.transfer.vo.PayTransferCreateReqVO;
import com.ccloud.module.pay.controller.admin.transfer.vo.PayTransferPageReqVO;
import com.ccloud.module.pay.controller.admin.transfer.vo.PayTransferRespVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.transfer.PayTransferDO;
import com.ccloud.module.pay.service.app.PayAppService;
import com.ccloud.module.pay.service.transfer.PayTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.PAY_TRANSFER_NOT_FOUND;

@Tag(name = "管理后台 - 转账单")
@RestController
@RequestMapping("/admin-api/pay/transfer")
@Validated
public class PayTransferController {

    @Resource
    private PayTransferService transferService;
    @Resource
    private PayAppService appService;

    @PostMapping("/create")
    @Operation(summary = "发起转账")
    @PreAuthorize("@ss.hasPermission('pay:transfer:create')")
    public CommonResult<Long> createTransfer(@Valid @RequestBody PayTransferCreateReqVO reqVO, HttpServletRequest request) {
        PayAppDO app = appService.validPayApp(reqVO.getAppId());
        PayTransferCreateReqDTO reqDTO = new PayTransferCreateReqDTO().setAppKey(app.getAppKey())
                .setChannelCode(reqVO.getChannelCode()).setMerchantTransferId("ADMIN-" + IdUtil.fastSimpleUUID())
                .setSubject(reqVO.getSubject()).setPrice(reqVO.getPrice()).setUserAccount(reqVO.getUserAccount())
                .setUserName(reqVO.getUserName()).setUserIp(ServletUtils.getClientIP(request));
        return success(transferService.createTransfer(reqDTO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得转账单分页")
    @PreAuthorize("@ss.hasPermission('pay:transfer:query')")
    public CommonResult<PageResult<PayTransferRespVO>> getTransferPage(@Valid PayTransferPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(transferService.getTransferPage(pageReqVO), PayTransferRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得转账单详情")
    @Parameter(name = "id", description = "转账单编号", required = true, example = "4096")
    @PreAuthorize("@ss.hasPermission('pay:transfer:query')")
    public CommonResult<PayTransferRespVO> getTransfer(@RequestParam("id") Long id) {
        PayTransferDO transfer = transferService.getTransfer(id);
        if (transfer == null) {
            throw exception(PAY_TRANSFER_NOT_FOUND);
        }
        return success(BeanUtils.toBean(transfer, PayTransferRespVO.class));
    }
}
