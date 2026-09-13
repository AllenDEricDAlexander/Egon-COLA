package top.egon.cola.component.common.mybatis.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "EGON_MP_PG_MODEL_TEST", matches = "true")
class EgonColaOptimisticLockPostgreSqlTest {

    @Test
    void staleUpdateAndStaleDeleteCannotOverwriteANewerVersion() throws Exception {
        try (var fixture = EgonColaLogicDeletePostgreSqlTest.fixture()) {
            var repository = fixture.repository();
            TestBusinessModel inserted = new TestBusinessModel().businessValues("before", null);
            assertThat(repository.save(inserted)).isTrue();
            TestBusinessModel first = repository.getById(inserted.getId());
            TestBusinessModel staleUpdate = repository.getById(inserted.getId());
            TestBusinessModel staleDelete = repository.getById(inserted.getId());
            first.setTitle("winner");
            assertThat(repository.updateById(first)).isTrue();
            staleUpdate.setTitle("loser");
            assertThat(repository.updateById(staleUpdate)).isFalse();
            assertThat(repository.removeById(staleDelete)).isFalse();
            assertThat(repository.getById(inserted.getId()).getTitle()).isEqualTo("winner");
            assertThat(repository.getById(inserted.getId()).getVersion()).isEqualTo(1L);
        }
    }
}
