package top.egon.cola.component.ddc.model.vo;

import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;

import java.time.Instant;

public record DdcLeaseOperationResult(
        DdcLeaseOperationStatus status,
        Instant leaseExpireAt
) {

    public boolean renewed() {
        return status == DdcLeaseOperationStatus.RENEWED;
    }

    public boolean deleted() {
        return status == DdcLeaseOperationStatus.DELETED;
    }
}
