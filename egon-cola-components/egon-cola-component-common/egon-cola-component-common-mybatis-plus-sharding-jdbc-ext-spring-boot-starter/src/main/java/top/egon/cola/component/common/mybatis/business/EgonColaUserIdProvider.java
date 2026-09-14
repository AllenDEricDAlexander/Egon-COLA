package top.egon.cola.component.common.mybatis.business;

/**
 * Supplies the trusted current audit user identifier.
 */
@FunctionalInterface
public interface EgonColaUserIdProvider {

    String currentUserId();
}
