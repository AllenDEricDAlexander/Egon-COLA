package top.egon.cola.component.common.mybatis.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an infrastructure mapper whose SQL statements provide their own explicit tenant scope.
 *
 * <p>This marker permits its table to be excluded from automatic TenantLine rewriting. Read, update
 * and delete statements must carry their own tenant predicate; insert statements must supply the
 * intended tenant value. It does not install this scope or disable the other MyBatis-Plus guards.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EgonColaExplicitTenantScopeMapper {
}
