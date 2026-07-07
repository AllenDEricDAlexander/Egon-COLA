package top.egon.cola.component.ddc.client;

import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.util.List;

public interface DdcAdminClient {

    void register(DdcInstanceRegisterRequest request);

    void heartbeat(DdcHeartbeatRequest request);

    void offline(DdcHeartbeatRequest request);

    List<DdcConfigValue> pull();

    void reportDefaults(DdcDefaultReportRequest request);

    void ack(DdcAckRequest request);
}
