package top.egon.cola.component.tianshu.admin.service.ai.job;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiSystemActor;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * AI 作业的逐租户执行模板：校验租户绑定未变化，并在租户上下文与系统主体下运行。
 * / Per-tenant template of AI jobs: verifies the binding is unchanged and runs inside the tenant context as a system
 * actor.
 */
@Slf4j
public class DdcAiTenantJobRunner {

    private final DdcTenantAdminProperties tenantProperties;
    private final DdcTenantRegistry tenantRegistry;
    private final DdcTenantContextExecutor tenantContextExecutor;

    public DdcAiTenantJobRunner(DdcTenantAdminProperties tenantProperties, DdcTenantRegistry tenantRegistry,
            DdcTenantContextExecutor tenantContextExecutor) {
        this.tenantProperties = Objects.requireNonNull(tenantProperties, "tenantProperties");
        this.tenantRegistry = Objects.requireNonNull(tenantRegistry, "tenantRegistry");
        this.tenantContextExecutor = Objects.requireNonNull(tenantContextExecutor, "tenantContextExecutor");
    }

    /** 对每个启用租户执行；单租户失败只记录并继续。 / Runs for every enabled tenant; one tenant's failure is logged. */
    public int forEachTenant(String actor, String jobName, Supplier<Integer> body) {
        int total = 0;
        for (DdcTenantBindingDTO configured : tenantProperties.getBindings()) {
            if (configured == null || !configured.isEnabled()) continue;
            try {
                Integer count = inTenant(configured, actor, body);
                total += count == null ? 0 : count;
            } catch (RuntimeException failure) {
                log.error("{} failed for tenantId={}", jobName, configured.getTenantId(), failure);
            }
        }
        return total;
    }

    /** 在指定租户的上下文与系统主体下执行。 / Runs inside the tenant's context as the system actor. */
    public <T> T inTenant(DdcTenantBindingDTO configured, String actor, Supplier<T> body) {
        DdcTenantBindingDTO registered = tenantRegistry.requireEnabledTenant(configured.getTenantId());
        if (!registered.isEnabled()
                || !Objects.equals(registered.getIdentityTenantId(), configured.getIdentityTenantId())) {
            throw new IllegalStateException("TENANT_BINDING_CHANGED_DURING_SCAN");
        }
        DdcTenantContextDTO context = new DdcTenantContextDTO(
                registered.getTenantId(), registered.getIdentityTenantId(), actor);
        return tenantContextExecutor.call(context, () -> DdcAiSystemActor.bind(actor, body));
    }

    public Iterable<DdcTenantBindingDTO> bindings() {
        return tenantProperties.getBindings();
    }
}
