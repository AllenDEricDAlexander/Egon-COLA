package top.egon.cola.component.ddc.registry;

import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DdcActiveRegistrationIndex {

    private final Map<String, ActiveRegistration> registrations = new ConcurrentHashMap<>();

    void put(DdcServiceKey serviceKey, DdcLeaseSession session) {
        registrations.put(session.leaseId(), new ActiveRegistration(serviceKey, session.instanceId()));
    }

    DdcServiceKey require(String instanceId, String leaseId) {
        ActiveRegistration registration = registrations.get(leaseId);
        if (registration == null || !registration.instanceId().equals(instanceId)) {
            throw new DdcException(DdcErrorStatus.LEASE_MISMATCH);
        }
        return registration.serviceKey();
    }

    void remove(String leaseId) {
        registrations.remove(leaseId);
    }

    void clear() {
        registrations.clear();
    }

    private record ActiveRegistration(DdcServiceKey serviceKey, String instanceId) {
    }
}
