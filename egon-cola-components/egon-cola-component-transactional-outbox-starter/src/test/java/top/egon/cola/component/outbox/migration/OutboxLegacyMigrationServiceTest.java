package top.egon.cola.component.outbox.migration;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.persistence.OutboxTechnicalContextExecutor;
import top.egon.cola.component.outbox.persistence.converter.OutboxHeadersConverter;
import top.egon.cola.component.outbox.persistence.converter.OutboxMessageConverter;
import top.egon.cola.component.outbox.persistence.repository.OutboxMessageRepository;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OutboxLegacyMigrationServiceTest {

    private static ValidatorFactory validatorFactory;
    private static ValidationUtils validationUtils;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validationUtils = new ValidationUtils(validatorFactory.getValidator());
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void rejectsUnsafeSourceSchemaBeforeAccessingInfrastructure() {
        OutboxManagedDdlInitializer ddlInitializer = mock(OutboxManagedDdlInitializer.class);
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        OutboxLegacyMigrationService service = service(
                new OutboxMpStorageProperties().setMigrationMode(true), ddlInitializer, repository);

        assertThatThrownBy(() -> service.migrate("legacy;drop", 10, false))
                .isInstanceOf(OutboxConfigurationException.class)
                .hasMessage("OUTBOX_MIGRATION_SOURCE_SCHEMA_INVALID");
        verifyNoInteractions(ddlInitializer, repository);
    }

    @Test
    void refusesMigrationWhenTheMaintenanceFlagIsOffBeforeOpeningTheSource() {
        OutboxManagedDdlInitializer ddlInitializer = mock(OutboxManagedDdlInitializer.class);
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        OutboxLegacyMigrationService service = service(
                new OutboxMpStorageProperties().setMigrationMode(false), ddlInitializer, repository);

        assertThatThrownBy(() -> service.migrate("legacy_outbox", 10, false))
                .isInstanceOf(OutboxConfigurationException.class)
                .hasMessage("OUTBOX_MIGRATION_MODE_REQUIRED");
        verifyNoInteractions(ddlInitializer, repository);
    }

    @Test
    void rejectsAnInvalidBatchSizeBeforeOpeningTheSource() {
        OutboxManagedDdlInitializer ddlInitializer = mock(OutboxManagedDdlInitializer.class);
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        OutboxLegacyMigrationService service = service(
                new OutboxMpStorageProperties().setMigrationMode(true), ddlInitializer, repository);

        assertThatThrownBy(() -> service.migrate("legacy_outbox", 0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OUTBOX_MIGRATION_BATCH_SIZE_INVALID");
        verifyNoInteractions(ddlInitializer, repository);
    }

    private OutboxLegacyMigrationService service(
            OutboxMpStorageProperties properties,
            OutboxManagedDdlInitializer ddlInitializer,
            OutboxMessageRepository repository
    ) {
        return new OutboxLegacyMigrationService(
                ddlInitializer,
                "a".repeat(64),
                repository,
                mock(OutboxMessageConverter.class),
                mock(OutboxHeadersConverter.class),
                mock(OutboxMessageValidator.class),
                mock(OutboxTechnicalContextExecutor.class),
                properties,
                mock(TransactionTemplate.class),
                validationUtils
        );
    }
}
