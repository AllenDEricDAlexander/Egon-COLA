package top.egon.cola.component.ddc.admin.service.management;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;

import java.util.Comparator;
import java.util.List;

@Service
public class DdcRegistryAdminPageService {

    private static final Comparator<String> TEXT =
            Comparator.nullsFirst(String::compareTo);

    private static final Comparator<DdcManagementServiceKey> SERVICE_ORDER =
            Comparator.comparing(DdcManagementServiceKey::bizCode, TEXT)
                    .thenComparing(DdcManagementServiceKey::env, TEXT)
                    .thenComparing(DdcManagementServiceKey::appCode, TEXT)
                    .thenComparing(DdcManagementServiceKey::serviceKind, TEXT)
                    .thenComparing(DdcManagementServiceKey::protocol, TEXT)
                    .thenComparing(DdcManagementServiceKey::serviceName, TEXT)
                    .thenComparing(DdcManagementServiceKey::group, TEXT)
                    .thenComparing(DdcManagementServiceKey::version, TEXT)
                    .thenComparing(DdcManagementServiceKey::serviceId, TEXT);

    private static final Comparator<DdcManagementServiceInstance>
            INSTANCE_ORDER = Comparator.comparing(
                    DdcManagementServiceInstance::status, TEXT
            ).thenComparing(
                    DdcManagementServiceInstance::host, TEXT
            ).thenComparingInt(
                    DdcManagementServiceInstance::port
            ).thenComparing(
                    DdcManagementServiceInstance::instanceId, TEXT
            );

    private final DdcManagementFacade facade;

    public DdcRegistryAdminPageService(DdcManagementFacade facade) {
        this.facade = facade;
    }

    public Page<DdcManagementServiceKey> pageServices(
            DdcManagementServiceQuery query,
            PageQuery pageQuery
    ) {
        List<DdcManagementServiceKey> records = facade.getServiceKeys(query)
                .services().stream().sorted(SERVICE_ORDER).toList();
        return DdcAdminPageSupport.slice(records, pageQuery);
    }

    public Page<DdcManagementServiceInstance> pageInstances(
            DdcManagementServiceQuery query,
            PageQuery pageQuery
    ) {
        List<DdcManagementServiceInstance> records = facade.getInstances(query)
                .instances().stream().sorted(INSTANCE_ORDER).toList();
        return DdcAdminPageSupport.slice(records, pageQuery);
    }
}
