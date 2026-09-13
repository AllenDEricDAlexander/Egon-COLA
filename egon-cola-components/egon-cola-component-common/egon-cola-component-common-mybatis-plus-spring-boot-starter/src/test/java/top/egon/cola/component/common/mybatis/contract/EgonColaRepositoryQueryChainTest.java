package top.egon.cola.component.common.mybatis.contract;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.support.TestBusinessMapper;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EgonColaRepositoryQueryChainTest {

    @Test
    @SuppressWarnings("unchecked")
    void allFourQueryChainsRejectBeforeContextOrMapperAccessThroughEitherType() throws Exception {
        EgonColaRepository<TestBusinessMapper, TestBusinessModel> repository = mock(EgonColaRepository.class, CALLS_REAL_METHODS);
        IRepository<TestBusinessModel> upstream = repository;
        List<Runnable> calls = List.of(repository::query, repository::lambdaQuery, () -> repository.lambdaQuery(null),
                repository::ktQuery, upstream::query, upstream::lambdaQuery, () -> upstream.lambdaQuery(new TestBusinessModel()), upstream::ktQuery);
        for (Runnable call : calls) {
            assertThatThrownBy(call::run).isInstanceOf(UnsupportedOperationException.class).hasMessage("QUERY_CHAIN_FORBIDDEN");
        }
        verify(repository, never()).getBaseMapper();
        for (var method : EgonColaRepository.class.getDeclaredMethods()) {
            if (!method.isSynthetic() && List.of("query", "lambdaQuery", "ktQuery").contains(method.getName())) {
                assertThat(Modifier.isFinal(method.getModifiers())).isTrue();
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void unsafeWriteEntriesRejectWithoutSql() {
        EgonColaRepository<TestBusinessMapper, TestBusinessModel> repository = mock(EgonColaRepository.class, CALLS_REAL_METHODS);
        List<Runnable> calls = List.of(() -> repository.removeByMap(java.util.Map.of("id", 1L)),
                () -> repository.remove(null), () -> repository.update((com.baomidou.mybatisplus.core.conditions.Wrapper<TestBusinessModel>) null),
                repository::update, repository::lambdaUpdate, repository::ktUpdate);
        for (Runnable call : calls) {
            assertThatThrownBy(call::run).isInstanceOf(UnsupportedOperationException.class).hasMessage("UNSCOPED_WRITE_FORBIDDEN");
        }
        verify(repository, never()).getBaseMapper();
    }
}
