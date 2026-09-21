package top.egon.cola.archetype.source.lightopen.application.teaching.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.common.exception.TeachingUseCaseException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Teaching use-case rules; the claim itself belongs to the idempotency service. */
@Component("teachingApplicationValidator")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class TeachingApplicationValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void validate(CreateSchoolClassCommand command) {
        validateBean(command);
        requireText(command.name(), "INVALID_CLASS", "class name is required");
        requireText(command.semester(), "INVALID_SEMESTER", "semester is required");
        requireOperator(command.operatorId(), command.idempotencyKey());
    }

    public void validate(CreateCourseCommand command) {
        validateBean(command);
        requireText(command.code(), "INVALID_COURSE", "course code is required");
        requireText(command.name(), "INVALID_COURSE", "course name is required");
        requireOperator(command.operatorId(), command.idempotencyKey());
    }

    public void validate(ScheduleCourseCommand command) {
        validateBean(command);
        if (command.startsAt() == null || command.endsAt() == null
                || !command.startsAt().isBefore(command.endsAt())) {
            throw new TeachingUseCaseException("INVALID_SCHEDULE", "startsAt must be before endsAt");
        }
        requireOperator(command.operatorId(), command.idempotencyKey());
    }

    private void requireOperator(String operatorId, String idempotencyKey) {
        requireText(operatorId, "MISSING_OPERATOR", "operator context is required");
        requireText(idempotencyKey, "MISSING_IDEMPOTENCY_KEY", "idempotency key is required");
    }

    private void requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new TeachingUseCaseException(code, message);
        }
    }
}
