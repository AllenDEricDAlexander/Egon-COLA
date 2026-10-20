package top.egon.cola.component.dtp.trigger.listener;

import org.redisson.api.listener.MessageListener;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.registry.IRegistry;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @BelongsProject: egon-cola-component-dynamic-thread-pool
 * @BelongsPackage: top.egon.cola.component.dtp.trigger.listener
 * @ClassName: ThreadPoolConfigAdjustListener
 * @Author: egon
 * @CreateTime: 2025Year-05Month-11Day-下午 3:39
 * @Description: 动态线程池变更监听
 * @Version: 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ThreadPoolConfigAdjustListener implements MessageListener<DtpConfigChangeMessage> {

    @Qualifier("dynamicThreadPollService")
    private final IDynamicThreadPoolService dynamicThreadPoolService;

    @Qualifier("redisRegistry")
    private final IRegistry registry;

    private final String appName;
    private final String instanceId;

    private final Cache<ChangeKey, CompletableFuture<AppliedChange>> applied = CacheBuilder.newBuilder()
            .maximumSize(10000).expireAfterWrite(Duration.ofMinutes(10)).build();

    public ThreadPoolConfigAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        this(dynamicThreadPoolService, registry, null, null);
    }

    @Override
    public void onMessage(CharSequence channel, DtpConfigChangeMessage message) {
        if (message == null) { log.warn("动态线程池收到空配置消息 channel={}", channel); return; }
        if (appName != null && (!appName.equals(message.getAppName()) || !instanceId.equals(message.getInstanceId()))) {
            return;
        }
        ExecutorUpdateCommand command = message.getPayload();
        if (command == null || message.getMessageId() == null || message.getMessageId().isBlank()
                || !Objects.equals(message.getAppName(), command.getAppName())
                || !Objects.equals(message.getInstanceId(), command.getInstanceId())
                || !Objects.equals(message.getExecutorName(), command.getExecutorName())
                || !Objects.equals(message.getExecutorKind(), command.getExecutorKind())) {
            log.warn("动态线程池消息身份或参数不一致 channel={}", channel);
            return;
        }
        TraceContext traceContext = TraceContext.fromHeaders(name -> readTraceHeader(message, name), false);
        try (TraceContext.Scope ignored = traceContext.open()) {
            ChangeKey key = new ChangeKey(message.getAppName(), message.getInstanceId(), message.getExecutorName(), message.getMessageId());
            ChangeDefinition definition = ChangeDefinition.from(command);
            CompletableFuture<AppliedChange> pending = new CompletableFuture<>();
            CompletableFuture<AppliedChange> previous = applied.asMap().putIfAbsent(key, pending);
            if (previous != null) {
                AppliedChange existing = previous.join();
                if (!definition.equals(existing.definition)) {
                    log.warn("动态线程池重复消息内容冲突 app={} instance={} executor={} messageId={}",
                            message.getAppName(), message.getInstanceId(), message.getExecutorName(), message.getMessageId());
                }
                return;
            }
            UpdateResult result;
            try {
                result = Objects.requireNonNull(dynamicThreadPoolService.updateExecutor(command), "update result");
                pending.complete(new AppliedChange(definition, result));
            } catch (RuntimeException e) {
                pending.completeExceptionally(e);
                throw e;
            }
            try { registry.recordAuditEvent(buildAuditEvent(message, result, traceContext)); }
            catch (RuntimeException e) {
                log.error("动态线程池审计写入失败，实际调参结果不回滚 app={} instance={} executor={} messageId={} success={} cause={}",
                        message.getAppName(), message.getInstanceId(), message.getExecutorName(), message.getMessageId(), result.isSuccess(), e.getClass().getSimpleName());
            }
            if (result.getAfter() != null) {
                try { registry.reportSnapshot(result.getAfter()); }
                catch (RuntimeException e) {
                    log.error("动态线程池快照报告失败 app={} instance={} executor={} messageId={} cause={}",
                            message.getAppName(), message.getInstanceId(), message.getExecutorName(), message.getMessageId(), e.getClass().getSimpleName());
                }
            }
        } catch (RuntimeException e) {
            log.error("动态线程池配置处理失败 app={} instance={} executor={} messageId={} cause={}",
                    message.getAppName(), message.getInstanceId(), message.getExecutorName(), message.getMessageId(), e.getClass().getSimpleName());
        }
    }

    private record ChangeKey(String app, String instance, String executor, String messageId) { }

    private record ChangeDefinition(Integer core, Integer max, Long keepAlive, Boolean coreTimeout,
                                    Integer capacity, Integer concurrency, String operator, Long version) {
        private static ChangeDefinition from(ExecutorUpdateCommand command) {
            return new ChangeDefinition(command.getCorePoolSize(), command.getMaximumPoolSize(), command.getKeepAliveSeconds(),
                    command.getAllowCoreThreadTimeOut(), command.getQueueCapacity(), command.getConcurrencyLimit(),
                    command.getOperator(), command.getVersion());
        }
    }

    private static final class AppliedChange {
        private final ChangeDefinition definition;
        private final UpdateResult result;
        private AppliedChange(ChangeDefinition definition, UpdateResult result) {
            this.definition = definition;
            this.result = result;
        }
    }

    private DtpAuditEvent buildAuditEvent(DtpConfigChangeMessage message,
                                          UpdateResult result,
                                          TraceContext traceContext) {
        DtpAuditEvent event = new DtpAuditEvent();
        event.setEventId(message.getMessageId());
        event.setTraceId(traceContext.traceId());
        event.setRequestId(traceContext.requestId());
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

    private String readTraceHeader(DtpConfigChangeMessage message, String name) {
        if (TraceContext.TRACEPARENT_HEADER.equals(name)) {
            return message.getTraceparent();
        }
        if (TraceContext.TRACESTATE_HEADER.equals(name)) {
            return message.getTracestate();
        }
        if (TraceContext.REQUEST_ID_HEADER.equals(name)) {
            return message.getRequestId();
        }
        return null;
    }

}
