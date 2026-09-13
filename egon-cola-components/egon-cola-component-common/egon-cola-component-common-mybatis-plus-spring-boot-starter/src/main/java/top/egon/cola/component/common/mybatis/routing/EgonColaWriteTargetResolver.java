package top.egon.cola.component.common.mybatis.routing;

/**
 * Resolves the primary groups and physical tables used by the final SQL write guard.
 * Adapters must share the datasource's immutable routing policy and reject unknown tables.
 */
@FunctionalInterface
public interface EgonColaWriteTargetResolver {

    EgonColaRouteResult resolve(EgonColaRouteQuery query);
}
