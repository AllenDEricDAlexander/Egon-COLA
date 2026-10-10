package top.egon.cola.platform.tianquan.jianshen.starter.field;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.Set;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import org.springframework.security.authorization.AuthorizationDeniedException;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;

/** Explicit server-side write permission boundary; no DTO filtering or persistence. */
@Slf4j
@RequiredArgsConstructor
@Validated(FieldWriteAuthorizationCommand.WriteFields.class)
public class Rbac3FieldWriteAuthorizationComponent {
    @Qualifier("authorizationService") private final AuthorizationService authorization;
    @Qualifier("currentRbac3User") private final CurrentRbac3User currentUser;
    @Qualifier("fieldWriteAuthorizationConverterImpl") private final FieldWriteAuthorizationConverter converter;
    @Qualifier("defaultValidator") private final Validator validator;

    public FieldPolicyDecision requireWritableFields(@Valid FieldWriteAuthorizationCommand command) {
        ValidationUtils schema = new ValidationUtils(validator);
        schema.validate(command, FieldWriteAuthorizationCommand.WriteFields.class);
        Set<String> submitted = command.getSubmittedFields();
        var identity = currentUser.require().identity();
        var request = schema.validate(converter.toTarget(command));
        FieldPolicyDecision decision = schema.validate(authorization.decideFields(request));
        if (decision.decision() == Decision.INDETERMINATE) {
            throw new AuthorizationService.RuntimeUnavailableException(decision.reasonCode(), identity);
        }
        if (decision.decision() != Decision.ALLOW || submitted.stream().anyMatch(field -> {
            var access = decision.fields().get(field);
            return access == null || access.level() != FieldAccessLevel.WRITE;
        })) {
            throw new AuthorizationDeniedException("FIELD_ACCESS_DENIED");
        }
        return decision;
    }
}
