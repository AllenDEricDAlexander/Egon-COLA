package top.egon.cola.platform.tianquan.shoubing.admin.audit.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter.IdentityAuditViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.dto.IdentityAuditQueryDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.vo.IdentityAuditPageVO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;

class IdentityAuditServiceImplTest {

    private static final TrustedRealmBO PLATFORM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example/platform"))
            .realmKey("platform")
            .realmType(RealmTypeEnum.PLATFORM)
            .storagePartitionId(9000L)
            .trustVersion(0L)
            .build();

    private final OAuthAuditRepository audits = mock(OAuthAuditRepository.class);
    private final OAuthManagementRealmResolver realmResolver = mock(OAuthManagementRealmResolver.class);
    private final IdentityAuditViewConverter views = Mappers.getMapper(IdentityAuditViewConverter.class);
    private final IdentityAuditServiceImpl service = new IdentityAuditServiceImpl(audits, views, realmResolver);

    @Test
    void usesPlatformMpRepositoryAndPreservesTheSafeAuditView() {
        IdentityAuditQueryDTO query = new IdentityAuditQueryDTO(0, 20, "admin-sub", null, "FAILURE",
                null, null, "trace-1");
        OAuthAuditBO audit = OAuthAuditBO.builder()
                .publicId("audit-1")
                .eventType("IDENTITY_LOGIN_FAILED")
                .actorSub("admin-sub")
                .targetSub("alice-sub")
                .result("FAILURE")
                .reasonCode("BAD_CREDENTIALS")
                .occurredAt(Instant.parse("2026-10-08T08:00:00Z"))
                .auditKind("IDENTITY")
                .payload(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode())
                .build();
        when(realmResolver.platformRealm()).thenReturn(PLATFORM);
        when(audits.countIdentityPage(PLATFORM, query)).thenReturn(21L);
        when(audits.findIdentityPage(PLATFORM, query)).thenReturn(List.of(audit));

        IdentityAuditPageVO result = service.list(query);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(21L);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.content()).singleElement().satisfies(view -> {
            assertThat(view.id()).isEqualTo("audit-1");
            assertThat(view.eventType()).isEqualTo("IDENTITY_LOGIN_FAILED");
            assertThat(view.actorSub()).isEqualTo("admin-sub");
            assertThat(view.targetSub()).isEqualTo("alice-sub");
            assertThat(view.result()).isEqualTo("FAILURE");
            assertThat(view.reason()).isEqualTo("BAD_CREDENTIALS");
        });
        verify(audits).countIdentityPage(PLATFORM, query);
        verify(audits).findIdentityPage(PLATFORM, query);
    }

    @Test
    void rejectsInvalidPagesBeforeLookingUpThePlatformRealm() {
        assertThatThrownBy(() -> service.list(new IdentityAuditQueryDTO(-1, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid audit page request");
        assertThatThrownBy(() -> service.list(new IdentityAuditQueryDTO(0, 201)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid audit page request");
        verify(realmResolver, never()).platformRealm();
        verify(audits, never()).findIdentityPage(any(), any());
    }
}
