package top.egon.cola.component.ddc.management;

import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;

import java.util.List;

public interface DdcManagementClient {

    DdcManagementConfig upsert(DdcManagementConfigUpsertRequest request);

    void delete(DdcManagementConfigDeleteRequest request);

    DdcManagementPublishResult publish(DdcManagementPublishRequest request);

    DdcManagementPublishTask getPublishTask(String changeId);

    DdcManagementPublishResult retry(String changeId);

    List<DdcManagementConfigClientInstance> getConfigClients(
            DdcManagementInstanceQuery query);

    DdcManagementServiceCatalog getServiceKeys(DdcManagementServiceQuery query);

    DdcManagementServiceSnapshot getInstances(DdcManagementServiceQuery query);
}
