package com.ccloud.module.system.controller.admin.oauth2;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import com.ccloud.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenRespVO;
import com.ccloud.module.system.service.oauth2.OAuth2TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 在线令牌管理：查看在线会话、强制下线
 */
@Tag(name = "管理后台 - OAuth2 令牌")
@RestController
@RequestMapping("/admin-api/system/oauth2-token")
@Validated
public class OAuth2TokenController {

    @Resource
    private OAuth2TokenService oauth2TokenService;

    @GetMapping("/page")
    @Operation(summary = "获得访问令牌分页", description = "只返回未过期的访问令牌")
    @PreAuthorize("@ss.hasPermission('system:oauth2-token:page')")
    public CommonResult<PageResult<OAuth2AccessTokenRespVO>> getAccessTokenPage(@Valid OAuth2AccessTokenPageReqVO reqVO) {
        return success(BeanUtils.toBean(oauth2TokenService.getAccessTokenPage(reqVO), OAuth2AccessTokenRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除访问令牌", description = "即强制该会话下线")
    @Parameter(name = "accessToken", description = "访问令牌", required = true, example = "e1b0a9f3c6d54ef1a6b0a1c2d3e4f5a6")
    @PreAuthorize("@ss.hasPermission('system:oauth2-token:delete')")
    public CommonResult<Boolean> deleteAccessToken(@RequestParam("accessToken") String accessToken) {
        oauth2TokenService.removeAccessToken(accessToken);
        return success(true);
    }
}
