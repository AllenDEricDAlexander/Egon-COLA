package top.egon.cola.component.ddc.admin.service.config;

import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.service.lease.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.util.List;

/**
 * 收敛 DDC 配置客户端运行时用例的应用门面。
 * / Application facade that consolidates DDC configuration-client runtime use cases.
 */
@Service
public class DdcConfigFacade {

    private final DdcInstanceAdminService instanceAdminService;
    private final DdcConfigService configService;
    private final DdcPublishService publishService;

    /**
     * 创建配置运行时门面。 / Creates the configuration-runtime facade.
     *
     * @param instanceAdminService 配置客户端租约服务 / configuration-client lease service
     * @param configService        配置查询服务 / configuration query service
     * @param publishService       发布确认服务 / publication acknowledgement service
     */
    public DdcConfigFacade(
            DdcInstanceAdminService instanceAdminService,
            DdcConfigService configService,
            DdcPublishService publishService) {
        this.instanceAdminService = instanceAdminService;
        this.configService = configService;
        this.publishService = publishService;
    }

    /** 注册配置客户端。 / Registers a configuration client. */
    public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
        return instanceAdminService.register(request);
    }

    /** 续期配置客户端租约。 / Renews a configuration-client lease. */
    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
        return instanceAdminService.heartbeat(request);
    }

    /** 注销配置客户端租约。 / Takes a configuration-client lease offline. */
    public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
        return instanceAdminService.offline(request);
    }

    /** 拉取物理作用域配置。 / Pulls configurations for a physical scope. */
    public List<DdcConfigValue> pull(
            String bizCode,
            String env,
            String appCode) {
        return configService.pull(bizCode, env, appCode);
    }

    /** 处理发布确认。 / Handles a publication acknowledgement. */
    public DdcPublishResultVO ack(DdcAckRequest request) {
        return publishService.ack(request);
    }
}
