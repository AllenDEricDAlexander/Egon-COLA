package top.egon.cola.component.common.mybatis.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteResult;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;

import java.sql.Connection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Final SQL and physical-write boundary, scoped to the Spring transaction rather than a thread lifetime. */
@Slf4j
@RequiredArgsConstructor
public final class EgonColaLocalWriteGuardInnerInterceptor implements InnerInterceptor {

    private static final Object WRITE_TARGET_KEY = new Object();

    @Qualifier("egonColaWriteTargetResolver")
    private final EgonColaWriteTargetResolver resolver;
    @Qualifier("egonColaTenantIdGuardInnerInterceptor")
    private final EgonColaTenantIdGuardInnerInterceptor sqlGuard;
    @Qualifier("egonColaTwoLevelRouteStrategy")
    private final EgonColaTwoLevelRouteStrategy strategy;
    @Qualifier("egonColaRoutingProfiles")
    private final Map<String, EgonColaRoutingProfileBO> profiles;

    @Override
    public void beforePrepare(StatementHandler handler, Connection connection, Integer timeout) {
        var wrapped = PluginUtils.mpStatementHandler(handler);
        var statement = wrapped.mappedStatement();
        Configuration factory = wrapped.configuration();
        List<EgonColaTenantIdGuardInnerInterceptor.ScopedRouteQuery> queries;
        try {
            queries = sqlGuard.validateFinal(statement, handler.getBoundSql(), profiles);
        } catch (RuntimeException failure) {
            markRollbackOnly(factory);
            throw failure;
        }
        boolean write = statement.getSqlCommandType() != SqlCommandType.SELECT;
        Set<String> groups = new LinkedHashSet<>();
        int targetCount = 0;
        try {
            for (EgonColaTenantIdGuardInnerInterceptor.ScopedRouteQuery scoped : queries) {
                EgonColaRouteQuery query = scoped.query();
                EgonColaRoutingProfileBO profile = profiles.get(query.logicalTable());
                if (write && profile != null && profile.kind() == EgonColaRoutingProfileBO.TableKindEnum.BROADCAST_READ_ONLY) {
                    throw new IllegalStateException("BROADCAST_READ_ONLY");
                }
                EgonColaRouteResult result = profile != null && profile.kind() != EgonColaRoutingProfileBO.TableKindEnum.TENANT_LEGACY
                        ? strategy.route(profile, query) : resolver.resolve(query);
                if (result == null || result.targets().isEmpty()) { throw new IllegalStateException("WRITE_TARGET_UNRESOLVED"); }
                if (scoped.schema() != null && result.targets().stream().anyMatch(target -> !scoped.schema().equals(target.schema()))) {
                    throw new IllegalStateException("SQL_SCHEMA_ROUTE_MISMATCH");
                }
                if (write) {
                    result.targets().forEach(target -> groups.add(target.group()));
                    targetCount += result.targets().size();
                }
            }
            if (!write || queries.isEmpty()) { return; }
            if (groups.size() != 1) { throw new IllegalStateException("LOCAL_WRITE_TARGET_MISMATCH"); }
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            if (!active) {
                if (targetCount != 1) { throw new IllegalStateException("LOCAL_TRANSACTION_REQUIRED"); }
                return;
            }
            WriteTargetBO previous = (WriteTargetBO) TransactionSynchronizationManager.getResource(WRITE_TARGET_KEY);
            String group = groups.iterator().next();
            if (previous != null && (previous.factory() != factory || !previous.group().equals(group))) {
                throw new IllegalStateException("LOCAL_WRITE_TARGET_MISMATCH");
            }
            Object resource = TransactionSynchronizationManager.getResource(factory.getEnvironment().getDataSource());
            if (!(resource instanceof ConnectionHolder holder) || !TransactionSynchronizationManager.isSynchronizationActive()) {
                throw new IllegalStateException("LOCAL_TRANSACTION_RESOURCE_MISSING");
            }
            if (previous == null) {
                WriteTargetBO state = new WriteTargetBO(factory, group, holder);
                TransactionSynchronizationManager.bindResource(WRITE_TARGET_KEY, state);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void suspend() { TransactionSynchronizationManager.unbindResourceIfPossible(WRITE_TARGET_KEY); }
                    @Override
                    public void resume() { TransactionSynchronizationManager.bindResource(WRITE_TARGET_KEY, state); }
                    @Override
                    public void afterCompletion(int status) { TransactionSynchronizationManager.unbindResourceIfPossible(WRITE_TARGET_KEY); }
                });
            }
        } catch (RuntimeException failure) {
            markRollbackOnly(factory);
            throw failure;
        }
    }

    private static void markRollbackOnly(Configuration factory) {
        Object previous = TransactionSynchronizationManager.getResource(WRITE_TARGET_KEY);
        if (previous instanceof WriteTargetBO state) { state.holder().setRollbackOnly(); }
        if (factory.getEnvironment() != null) {
            Object resource = TransactionSynchronizationManager.getResource(factory.getEnvironment().getDataSource());
            if (resource instanceof ConnectionHolder holder) { holder.setRollbackOnly(); }
        }
    }

    private record WriteTargetBO(@NotNull Configuration factory, @NotNull String group, @NotNull ConnectionHolder holder) {}
}
