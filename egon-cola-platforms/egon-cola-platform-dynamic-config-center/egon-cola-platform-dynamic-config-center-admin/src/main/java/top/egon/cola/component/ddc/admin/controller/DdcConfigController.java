package top.egon.cola.component.ddc.admin.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigQueryRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigRollbackRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigUpdateRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVersionVO;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/configs")
public class DdcConfigController {

    private final DdcConfigService configService;

    private final DdcPublishService publishService;

    public DdcConfigController(DdcConfigService configService, DdcPublishService publishService) {
        this.configService = configService;
        this.publishService = publishService;
    }

    @GetMapping
    public ResultRecord<List<DdcConfigVO>> list(DdcConfigQueryRequest request) {
        return ResultRecord.success(configService.list(request));
    }

    @PostMapping
    public ResultRecord<DdcConfigVO> create(@RequestBody DdcConfigCreateRequest request,
                                      @RequestParam(name = "operator", defaultValue = "system") String operator,
                                      Authentication authentication) {
        return ResultRecord.success(configService.create(
                request,
                trustedOperator(authentication, operator)
        ));
    }

    @PutMapping("/{id}")
    public ResultRecord<DdcConfigVO> update(@PathVariable("id") String id,
                                      @RequestBody DdcConfigUpdateRequest request,
                                      @RequestParam(name = "operator", defaultValue = "system") String operator,
                                      Authentication authentication) {
        request.setId(id);
        return ResultRecord.success(configService.update(
                request,
                trustedOperator(authentication, operator)
        ));
    }

    @DeleteMapping("/{id}")
    public ResultRecord<DdcConfigVO> delete(@PathVariable("id") String id,
                                      @RequestParam(name = "operator", defaultValue = "system") String operator,
                                      @RequestParam(name = "reason", defaultValue = "delete config") String reason,
                                      Authentication authentication) {
        return ResultRecord.success(configService.delete(
                id,
                trustedOperator(authentication, operator),
                reason
        ));
    }

    @PostMapping("/{id}/publish")
    public ResultRecord<DdcPublishResultVO> publish(@PathVariable("id") String id,
                                              @RequestBody DdcPublishRequest request,
                                              @RequestParam(name = "operator", defaultValue = "system") String operator,
                                              Authentication authentication) {
        DdcConfigVO config = configService.get(id);
        request.setBizCode(config.getBizCode());
        request.setAppCode(config.getAppCode());
        request.setEnv(config.getEnv());
        return ResultRecord.success(publishService.publish(
                request,
                trustedOperator(authentication, operator)
        ));
    }

    @GetMapping("/{id}/versions")
    public ResultRecord<List<DdcConfigVersionVO>> versions(@PathVariable("id") String id) {
        return ResultRecord.success(configService.versions(id));
    }

    @PostMapping("/{id}/rollback")
    public ResultRecord<DdcConfigVO> rollback(@PathVariable("id") String id,
                                        @RequestBody DdcConfigRollbackRequest request,
                                        @RequestParam(name = "operator", defaultValue = "system") String operator,
                                        Authentication authentication) {
        request.setConfigId(id);
        return ResultRecord.success(configService.rollback(
                request,
                trustedOperator(authentication, operator)
        ));
    }

    private String trustedOperator(
            Authentication authentication,
            String requestedOperator) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new IllegalStateException(
                    "Authenticated DDC Admin principal is required"
            );
        }
        String actor = auditValue(authentication.getName());
        if (actor.isBlank()) {
            throw new IllegalStateException(
                    "Authenticated DDC Admin principal is required"
            );
        }
        String trusted = "user:" + actor;
        if (requestedOperator == null || requestedOperator.isBlank()) {
            return trusted;
        }
        return trusted + " [requested="
                + auditValue(requestedOperator)
                + ']';
    }

    private String auditValue(String value) {
        String normalized = value
                .replaceAll("[\\p{Cntrl}]", " ")
                .trim();
        if (normalized.length() > 128) {
            return normalized.substring(0, 128);
        }
        return normalized;
    }
}
