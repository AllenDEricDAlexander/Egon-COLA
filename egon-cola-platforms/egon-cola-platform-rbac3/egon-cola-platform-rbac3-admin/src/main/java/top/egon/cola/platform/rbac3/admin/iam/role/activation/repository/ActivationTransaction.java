package top.egon.cola.platform.rbac3.admin.iam.role.activation.repository;

import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.dto.ReplaceCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.CurrentStateVO;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ResolvedActivationVO;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.TransactionResultVO;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.UserAuthorizationStateVO;

import java.time.Instant;
import java.util.function.Function;

/**
 * Transaction boundary for user-scoped role activation publication.
 */
public interface ActivationTransaction {

    TransactionResultVO replace(
            ReplaceCommandDTO command,
            Instant now,
            Function<UserAuthorizationStateVO, ResolvedActivationVO> resolutionFactory);

    CurrentStateVO current(
            String tenantId,
            String identitySub,
            String userId,
            Instant now);

    default void markFenced(String mutationId, Instant now) {
    }

    default void markCompleted(String mutationId, Instant now) {
    }

    default void markRecoveryRequired(String mutationId, String reasonCode, Instant now) {
    }
}
