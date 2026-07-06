package top.egon.cola.component.dtp.trigger.listener;

import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.registry.IRegistry;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;

import java.time.Instant;
import java.util.UUID;

/**
 * @BelongsProject: egon-cola-component-dynamic-thread-pool
 * @BelongsPackage: top.egon.cola.component.dtp.trigger.listener
 * @ClassName: ThreadPoolConfigAdjustListener
 * @Author: egon
 * @CreateTime: 2025Year-05Month-11Day-下午 3:39
 * @Description: 动态线程池变更监听
 * @Version: 1.0
 */
public class ThreadPoolConfigAdjustListener implements MessageListener<DtpConfigChangeMessage> {

    private final Logger logger = LoggerFactory.getLogger(ThreadPoolConfigAdjustListener.class);

    private final IDynamicThreadPoolService dynamicThreadPoolService;

    private final IRegistry registry;

    public ThreadPoolConfigAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registry = registry;
    }

    @Override
    public void onMessage(CharSequence channel, DtpConfigChangeMessage message) {
        if (message == null) {
            logger.warn("动态线程池，收到空配置变更消息。频道:{}", channel);
            MDC.clear();
            return;
        }
        try {
            MDC.put("traceId", message.getTraceId());
            MDC.put("requestId", message.getRequestId());
            ExecutorUpdateCommand command = message.getPayload();
            UpdateResult result = dynamicThreadPoolService.updateExecutor(command);
            DtpAuditEvent event = buildAuditEvent(message, result);
            registry.recordAuditEvent(event);
            if (result.getAfter() != null) {
                registry.reportSnapshot(result.getAfter());
            }
        } catch (Exception e) {
            logger.error("动态线程池，配置变更处理失败。应用:{} 实例:{} 执行器:{}",
                    message.getAppName(), message.getInstanceId(), message.getExecutorName(), e);
        } finally {
            MDC.clear();
        }
    }

    private DtpAuditEvent buildAuditEvent(DtpConfigChangeMessage message, UpdateResult result) {
        DtpAuditEvent event = new DtpAuditEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTraceId(message.getTraceId());
        event.setRequestId(message.getRequestId());
        event.setAppName(message.getAppName());
        event.setInstanceId(message.getInstanceId());
        event.setExecutorName(message.getExecutorName());
        event.setExecutorKind(message.getExecutorKind());
        event.setOperator(message.getOperator());
        event.setOperationType("UPDATE");
        event.setBeforeValue(result.getBefore());
        event.setAfterValue(result.getAfter());
        event.setSuccess(result.isSuccess());
        event.setErrorMessage(result.isSuccess() ? null : result.getMessage());
        event.setCreatedAt(Instant.now());
        return event;
    }

}
