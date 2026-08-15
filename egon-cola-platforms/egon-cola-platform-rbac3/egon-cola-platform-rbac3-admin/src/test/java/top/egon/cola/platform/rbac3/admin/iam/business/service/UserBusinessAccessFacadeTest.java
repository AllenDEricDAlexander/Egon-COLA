package top.egon.cola.platform.rbac3.admin.iam.business.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.command.ReplaceUserBusinessAccessesCommand;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.enums.UserBusinessAccessStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.vo.UserBusinessAccessVO;
import top.egon.cola.platform.rbac3.admin.iam.business.repository.UserBusinessAccessRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ExpectedVersionsVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationResultVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationScopeVO;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserBusinessAccessFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");

    private final UserBusinessAccessRepository store = mock(UserBusinessAccessRepository.class);
    private final DdcCatalogGateway catalog = mock(DdcCatalogGateway.class);
    private final AuthorizationMutationCoordinator mutationCoordinator =
            mock(AuthorizationMutationCoordinator.class);
    private final DatabaseClock databaseClock = mock(DatabaseClock.class);
    private final UserBusinessAccessFacade facade = new UserBusinessAccessFacade(
            store, catalog, mutationCoordinator, databaseClock);

    @Test
    void rejectsDisabledBusinessBeforeOpeningAuthorizationMutation() {
        when(catalog.findBusiness("biz-1")).thenReturn(Optional.of(
                new BusinessCatalogEntry("biz-1", "orders", "Orders", false)));

        assertThatThrownBy(() -> facade.replace(
                7L, 9L, "admin", command("biz-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");

        verifyNoInteractions(mutationCoordinator, store);
    }

    @Test
    void replacesManualGrantsInOneAuthorizationMutationAndEnrichesResponse() {
        when(databaseClock.transactionNow()).thenReturn(NOW);
        when(catalog.findBusiness("biz-1")).thenReturn(Optional.of(
                new BusinessCatalogEntry("biz-1", "orders", "Orders", true)));
        UserBusinessAccessVO raw = new UserBusinessAccessVO(
                "1001", "9", "biz-1", null, null, "ACTIVE",
                NOW, null, "MANUAL", "biz-1", "review", "T-1", 1L);
        when(mutationCoordinator.execute(
                any(MutationScopeVO.class), eq("9"), any(ExpectedVersionsVO.class), any()))
                .thenReturn(new MutationResultVO<>(
                        "mutation-1", true, "ALLOW", List.of(raw),
                        new ExpectedVersionsVO(null, null, null, null)));

        List<UserBusinessAccessVO> result = facade.replace(
                7L, 9L, "admin", command("biz-1"));

        assertThat(result).singleElement().satisfies(value -> {
            assertThat(value.ddcBusinessId()).isEqualTo("biz-1");
            assertThat(value.bizCode()).isEqualTo("orders");
            assertThat(value.bizName()).isEqualTo("Orders");
        });
        verify(mutationCoordinator).execute(
                any(MutationScopeVO.class), eq("9"), any(ExpectedVersionsVO.class), any());
    }

    private static ReplaceUserBusinessAccessesCommand command(String businessId) {
        return new ReplaceUserBusinessAccessesCommand(List.of(
                new ReplaceUserBusinessAccessesCommand.Item(
                        businessId, UserBusinessAccessStatusEnum.ACTIVE,
                        NOW, null, "review", "T-1", 0L)));
    }
}
