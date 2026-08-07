package top.egon.cola.component.dtp.registry.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.egon.cola.component.common.trace.TraceKeys;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author      有罗敷的马同学
 * @description 动态线程池配置变更消息
 * @Date        下午10:40 2026/6/29
 **/
@Getter
@Setter
@ToString
public class DtpConfigChangeMessage {

    private static final int MAX_CARRIER_VALUE_LENGTH = 512;

    private String messageId;

    private Map<String, String> traceContext = Map.of();

    private String appName;

    private String instanceId;

    private String executorName;

    private ExecutorKind executorKind;

    private ExecutorUpdateCommand payload;

    private String operator;

    private Instant timestamp;

    public void setTraceContext(Map<String, String> traceContext) {
        if (traceContext == null || traceContext.isEmpty()) {
            this.traceContext = Map.of();
            return;
        }
        Map<String, String> carrier = new LinkedHashMap<>();
        copyCarrierValue(traceContext, carrier, TraceKeys.TRACEPARENT_HEADER);
        copyCarrierValue(traceContext, carrier, TraceKeys.TRACESTATE_HEADER);
        copyCarrierValue(traceContext, carrier, TraceKeys.REQUEST_ID_HEADER);
        this.traceContext = Map.copyOf(carrier);
    }

    private void copyCarrierValue(Map<String, String> source, Map<String, String> target, String key) {
        String value = source.get(key);
        if (value != null && !value.isBlank() && value.length() <= MAX_CARRIER_VALUE_LENGTH) {
            target.put(key, value);
        }
    }

}
