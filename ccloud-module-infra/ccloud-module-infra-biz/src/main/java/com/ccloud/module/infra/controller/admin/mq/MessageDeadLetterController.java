package com.ccloud.module.infra.controller.admin.mq;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.infra.controller.admin.mq.vo.MessageDeadLetterPageReqVO;
import com.ccloud.module.infra.controller.admin.mq.vo.MessageDeadLetterRespVO;
import com.ccloud.module.infra.service.mq.MessageDeadLetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 消息死信")
@RestController
@RequestMapping("/admin-api/infra/message-dead-letter")
@Validated
public class MessageDeadLetterController {

    @Resource
    private MessageDeadLetterService deadLetterService;

    @GetMapping("/page")
    @Operation(summary = "获得消息死信分页", description = "覆盖全部租户")
    @PreAuthorize("@ss.hasPermission('infra:message-dead-letter:query')")
    public CommonResult<PageResult<MessageDeadLetterRespVO>> getDeadLetterPage(@Valid MessageDeadLetterPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(deadLetterService.getDeadLetterPage(pageReqVO), MessageDeadLetterRespVO.class));
    }

    @PutMapping("/update-handled")
    @Operation(summary = "标记死信已处理", description = "只做标记，不重新投递")
    @Parameter(name = "id", description = "死信编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:message-dead-letter:update')")
    public CommonResult<Boolean> updateDeadLetterHandled(@RequestParam("id") Long id) {
        deadLetterService.updateDeadLetterHandled(id);
        return success(true);
    }

    @PutMapping("/resend")
    @Operation(summary = "重新投递死信", description = "用原消息编号发回原主题，成功后标记为已处理")
    @Parameter(name = "id", description = "死信编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:message-dead-letter:update')")
    public CommonResult<Boolean> resendDeadLetter(@RequestParam("id") Long id) {
        deadLetterService.resendDeadLetter(id);
        return success(true);
    }
}
