package top.egon.cola.platform.tianquan.shoubing.admin.identity.repo;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

import java.util.Objects;
import java.util.function.Supplier;

@Component("identityDirectoryContext")
public class IdentityDirectoryContext {

    private static final String DEFAULT_USER_MDC_KEY = "userId";
    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    private final long directoryPartitionId;
    private final String tenantMdcKey;
    private final String userMdcKey;
    private final String serviceActor;

    @Autowired
    public IdentityDirectoryContext(
            @Value("${egon.tianquan-shoubing.identity-directory.platform-storage-partition-id}")
            long directoryPartitionId,
            @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
            EgonColaMybatisPlusProperties properties) {
        this(directoryPartitionId,
                properties.getTenantId().getMdcKey(),
                properties.getAudit().getUserIdMdcKey(),
                SERVICE_ACTOR);
    }

    public IdentityDirectoryContext(long directoryPartitionId, String tenantMdcKey) {
        this(directoryPartitionId, tenantMdcKey, DEFAULT_USER_MDC_KEY, SERVICE_ACTOR);
    }

    public IdentityDirectoryContext(
            long directoryPartitionId,
            String tenantMdcKey,
            String userMdcKey,
            String serviceActor) {
        if (directoryPartitionId <= 0L) {
            throw new IllegalArgumentException("IDENTITY_DIRECTORY_PARTITION_ID_INVALID");
        }
        if (tenantMdcKey == null || tenantMdcKey.isBlank()
                || userMdcKey == null || userMdcKey.isBlank()
                || tenantMdcKey.equals(userMdcKey)) {
            throw new IllegalArgumentException("IDENTITY_DIRECTORY_MDC_KEYS_INVALID");
        }
        if (serviceActor == null || serviceActor.isBlank()) {
            throw new IllegalArgumentException("IDENTITY_DIRECTORY_SERVICE_ACTOR_REQUIRED");
        }
        this.directoryPartitionId = directoryPartitionId;
        this.tenantMdcKey = tenantMdcKey;
        this.userMdcKey = userMdcKey;
        this.serviceActor = serviceActor;
    }

    public long directoryPartitionId() {
        return directoryPartitionId;
    }

    public <T> T withPlatformDirectoryPartition(Supplier<T> operation) {
        return withPlatformDirectoryPartition(null, operation);
    }

    public <T> T withPlatformDirectoryPartition(String actor, Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation");
        String previousTenant = MDC.get(tenantMdcKey);
        String previousUser = MDC.get(userMdcKey);
        MDC.put(tenantMdcKey, Long.toString(directoryPartitionId));
        if (actor != null && !actor.isBlank()) {
            MDC.put(userMdcKey, actor.trim());
        } else if (previousUser == null || previousUser.isBlank()) {
            MDC.put(userMdcKey, serviceActor);
        }
        try {
            return operation.get();
        } finally {
            if (previousTenant == null) {
                MDC.remove(tenantMdcKey);
            } else {
                MDC.put(tenantMdcKey, previousTenant);
            }
            if (previousUser == null) {
                MDC.remove(userMdcKey);
            } else {
                MDC.put(userMdcKey, previousUser);
            }
        }
    }
}
