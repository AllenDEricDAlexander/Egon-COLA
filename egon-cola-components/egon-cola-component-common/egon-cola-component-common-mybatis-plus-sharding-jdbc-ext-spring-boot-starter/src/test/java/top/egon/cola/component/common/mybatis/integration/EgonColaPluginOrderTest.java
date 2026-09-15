package top.egon.cola.component.common.mybatis.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EgonColaPluginOrderTest {

    @Test
    void originalScopeRejectsSystemPredicatesBeforeTheOptimisticPluginCanAddAnything() throws Throwable {
        EgonColaOriginalSqlGuardInterceptor guard = new EgonColaOriginalSqlGuardInterceptor(new EgonColaMybatisPlusProperties());
        for (String where : List.of("", " WHERE 1=1", " WHERE version=?", " WHERE tenant_id=?", " WHERE id>0", " WHERE id=? OR 1=1")) {
            var configuration = new MybatisConfiguration();
            var mapping = new ParameterMapping.Builder(configuration, "value", Long.class).build();
            var statement = new MappedStatement.Builder(configuration, "test.Mapper.update", new StaticSqlSource(configuration,
                    "UPDATE records SET title='changed'" + where, where.contains("?") ? List.of(mapping) : List.of()), SqlCommandType.UPDATE).build();
            Executor executor = mock(Executor.class);
            assertThatThrownBy(() -> guard.intercept(new Invocation(executor, Executor.class.getMethod("update", MappedStatement.class, Object.class),
                    new Object[]{statement, Map.of("value", 1L)}))).hasMessageContaining("BUSINESS_PREDICATE_REQUIRED");
            verifyNoInteractions(executor);
        }
    }

    @Test
    void entityIdIsNotASubstituteForAMatchingOriginalWherePredicate() throws Throwable {
        var guard = new EgonColaOriginalSqlGuardInterceptor(new EgonColaMybatisPlusProperties());
        var configuration = new MybatisConfiguration();
        TestBusinessModel entity = new TestBusinessModel().businessValues("changed", null);
        entity.setId(1L);
        entity.setVersion(0L);
        var mapping = new ParameterMapping.Builder(configuration, "id", Long.class).build();
        var statement = new MappedStatement.Builder(configuration, "test.Mapper.update", new StaticSqlSource(configuration,
                "UPDATE records SET title='changed' WHERE id=?", List.of(mapping)), SqlCommandType.UPDATE).build();
        Executor executor = mock(Executor.class);
        assertThatThrownBy(() -> guard.intercept(new Invocation(executor, Executor.class.getMethod("update", MappedStatement.class, Object.class),
                new Object[]{statement, Map.of("et", entity, "id", 2L)}))).hasMessageContaining("WRITE_ID_MISMATCH");
        verifyNoInteractions(executor);
        guard.intercept(new Invocation(executor, Executor.class.getMethod("update", MappedStatement.class, Object.class),
                new Object[]{statement, Map.of("et", entity, "id", 1L)}));
        verify(executor).update(statement, Map.of("et", entity, "id", 1L));
    }

    @Test
    void wrapperCompositionRunsTheOriginalGuardOutermost() throws Exception {
        var configuration = new MybatisConfiguration();
        TestBusinessModel entity = new TestBusinessModel().businessValues("changed", null);
        entity.setId(1L);
        entity.setVersion(0L);
        var statement = new MappedStatement.Builder(configuration, "test.Mapper.update", new StaticSqlSource(configuration,
                "UPDATE records SET title='changed'"), SqlCommandType.UPDATE).build();
        var mp = new MybatisPlusInterceptor();
        OptimisticLockerInnerInterceptor optimistic = spy(new OptimisticLockerInnerInterceptor());
        mp.addInnerInterceptor(optimistic);
        var guard = new EgonColaOriginalSqlGuardInterceptor(new EgonColaMybatisPlusProperties());
        Executor executor = (Executor) guard.plugin(mp.plugin(mock(Executor.class)));
        assertThatThrownBy(() -> executor.update(statement, new HashMap<>(Map.of("et", entity))))
                .hasMessageContaining("BUSINESS_PREDICATE_REQUIRED");
        verifyNoInteractions(optimistic);
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    void populatedUpdateWrappersCannotBeReused() throws Throwable {
        var configuration = new MybatisConfiguration();
        var mapping = new ParameterMapping.Builder(configuration, "id", Long.class).build();
        var statement = new MappedStatement.Builder(configuration, "test.Mapper.update", new StaticSqlSource(configuration,
                "UPDATE records SET title='changed' WHERE id=?", List.of(mapping)), SqlCommandType.UPDATE).build();
        var guard = new EgonColaOriginalSqlGuardInterceptor(new EgonColaMybatisPlusProperties());
        var wrapper = com.baomidou.mybatisplus.core.toolkit.Wrappers.<TestBusinessModel>update().eq("id", 1L);
        Map<String, Object> parameters = Map.of("id", 1L, "ew", wrapper);
        Executor executor = mock(Executor.class);
        var invocation = new Invocation(executor, Executor.class.getMethod("update", MappedStatement.class, Object.class),
                new Object[]{statement, parameters});
        guard.intercept(invocation);
        assertThatThrownBy(() -> guard.intercept(invocation)).hasMessage("WRITE_WRAPPER_REUSED");
        verify(executor, times(1)).update(statement, parameters);
    }
}
