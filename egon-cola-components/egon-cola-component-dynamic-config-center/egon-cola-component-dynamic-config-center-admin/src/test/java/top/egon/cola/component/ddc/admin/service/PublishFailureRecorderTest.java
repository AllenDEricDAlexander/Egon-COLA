package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PublishFailureRecorder.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_failure_recorder_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.flyway.enabled=false"
})
class PublishFailureRecorderTest {

    @Autowired
    private PublishFailureRecorder failureRecorder;

    @Autowired
    private DdcPublishTaskRepository publishTaskRepository;

    @Test
    void createsFailedRecordWhenTaskDoesNotExist() {
        failureRecorder.recordFailure("c1", "demo", "dev", "default", "switch", "database failed");

        assertThat(publishTaskRepository.findByChangeId("c1"))
                .get()
                .satisfies(task -> {
                    assertThat(task.getStatus()).isEqualTo(PublishStatus.FAILED.name());
                    assertThat(task.getErrorMessage()).isEqualTo("database failed");
                });
    }
}
