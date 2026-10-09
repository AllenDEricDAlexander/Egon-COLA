package top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.dto.CreateIdentityUserDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthPersistenceConfiguration;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthSubjectRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.TokenRevocationServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.support.runtime.IdpPlatformConfiguration;
import top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.service.IdentityOutboxPublisher;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEvent;

class IdentityAuthenticationTransactionContractTest {

    @Test
    void identityPublisherUsesOAuthAuditAndComponentOutboxOnly() {
        List<Class<?>> dependencies = Arrays.stream(IdentityOutboxPublisher.class.getDeclaredConstructors())
                .filter(constructor -> java.lang.reflect.Modifier.isPublic(constructor.getModifiers()))
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .toList();

        assertThat(dependencies).contains(TransactionalOutbox.class, OAuthAuditRepository.class,
                OAuthManagementRealmResolver.class);
        assertThat(dependencies).extracting(Class::getName).doesNotContain(
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.repo.IdentityAuditLogRepository",
                "top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.repo.IdentityOutboxEventRepository");
        assertThat(Arrays.stream(IdpPlatformConfiguration.class.getDeclaredMethods())
                .map(Method::getName)).doesNotContain("identityOutboxPublisher");
        List<Class<?>> platformConfigurationDependencies = Arrays.stream(
                        IdpPlatformConfiguration.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .toList();
        assertThat(platformConfigurationDependencies).contains(OutboxStore.class);
        assertThat(platformConfigurationDependencies).extracting(Class::getName).doesNotContain(
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.repo.IdentityAuditLogRepository",
                "top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.repo.IdentityOutboxEventRepository");
    }

    @Test
    void platformConfigurationEnablesTheSecuritySchedulers() {
        assertThat(IdpPlatformConfiguration.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
    }

    @Test
    void identityAndSecurityWritesResolveToTheSamePrimaryDataSourceTransactionManager() throws Exception {
        Method transactionManager = OAuthPersistenceConfiguration.class.getDeclaredMethod(
                "transactionManager", DataSource.class);
        Bean bean = transactionManager.getAnnotation(Bean.class);
        assertThat(bean.value()).containsExactly("transactionManager");
        assertThat(transactionManager.isAnnotationPresent(Primary.class)).isTrue();
        assertThat(transactionManager.getReturnType()).isEqualTo(DataSourceTransactionManager.class);

        Transactional create = IdentityUserServiceImpl.class
                .getMethod("create", CreateIdentityUserDTO.class)
                .getAnnotation(Transactional.class);
        Transactional publishAudit = IdentityOutboxPublisher.class
                .getMethod("append", IdentitySecurityEvent.class)
                .getAnnotation(Transactional.class);
        Transactional revokeSubject = TokenRevocationServiceImpl.class
                .getMethod("revokeSubject", OAuthSubjectRevokeCommand.class)
                .getAnnotation(Transactional.class);
        assertThat(create).isNotNull();
        assertThat(publishAudit).isNotNull();
        assertThat(publishAudit.transactionManager()).isEmpty();
        assertThat(revokeSubject.transactionManager()).isEqualTo("transactionManager");
    }
}
