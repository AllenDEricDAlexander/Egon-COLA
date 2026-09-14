package top.egon.cola.component.common.mybatis.business;

/**
 * Supplies the trusted current tenant identifier for one persistence operation.
 */
@FunctionalInterface
public interface EgonColaTenantIdProvider {

    Long currentTenantId();
}
