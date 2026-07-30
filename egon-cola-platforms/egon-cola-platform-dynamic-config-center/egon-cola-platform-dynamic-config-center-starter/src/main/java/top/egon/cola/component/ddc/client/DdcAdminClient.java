package top.egon.cola.component.ddc.client;

import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.List;

public interface DdcAdminClient {

    DdcLeaseSession register(DdcInstanceRegisterRequest request);

    DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request);

    DdcLeaseOperationResult offline(DdcHeartbeatRequest request);

    List<DdcConfigValue> pull();

    void reportDefaults(DdcDefaultReportRequest request);

    void ack(DdcAckRequest request);
}
