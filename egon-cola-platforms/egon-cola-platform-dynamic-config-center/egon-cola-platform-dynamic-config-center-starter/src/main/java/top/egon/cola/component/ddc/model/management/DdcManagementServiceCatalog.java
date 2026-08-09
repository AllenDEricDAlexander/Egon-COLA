package top.egon.cola.component.ddc.model.management;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * 某次观测得到的服务键目录。 / Catalog of service keys captured by one observation.
 *
 * @param generation 服务注册表代次 / service-registry generation
 * @param observedAt 目录观测时间 / catalog observation time
 * @param services   目录中的服务键 / service keys in the catalog
 */
public record DdcManagementServiceCatalog(
        long generation,
        Instant observedAt,
        @Nullable List<DdcManagementServiceKey> services
) {

    /**
     * 构造服务目录并将服务键列表归一化为不可变列表。 /
     * Constructs a service catalog and normalizes the service-key list to an immutable list.
     */
    public DdcManagementServiceCatalog {
        services = services == null ? List.of() : List.copyOf(services);
    }
}
