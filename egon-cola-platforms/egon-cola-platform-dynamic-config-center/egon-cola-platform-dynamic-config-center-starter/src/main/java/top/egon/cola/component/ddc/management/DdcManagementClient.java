package top.egon.cola.component.ddc.management;

import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.management.model.DdcManagementScopeQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;

import java.util.List;
import java.util.Optional;

public interface DdcManagementClient {

    default Optional<DdcManagementConfig> findConfig(DdcManagementConfigQuery query) {
        throw new UnsupportedOperationException("Exact config lookup is not supported");
    }

    DdcManagementConfig upsert(DdcManagementConfigUpsertRequest request);

    void delete(DdcManagementConfigDeleteRequest request);

    DdcManagementPublishResult publish(DdcManagementPublishRequest request);

    DdcManagementPublishTask getPublishTask(String changeId);

    DdcManagementPublishResult retry(String changeId);

    List<DdcManagementConfigClientInstance> getConfigClients(
            DdcManagementInstanceQuery query);

    default List<DdcManagementScopeBinding> getScopeBindings(
            DdcManagementScopeQuery query) {
        throw new UnsupportedOperationException(
                "Scope binding lookup is not supported");
    }

    DdcManagementServiceCatalog getServiceKeys(DdcManagementServiceQuery query);

    DdcManagementServiceSnapshot getInstances(DdcManagementServiceQuery query);
}
