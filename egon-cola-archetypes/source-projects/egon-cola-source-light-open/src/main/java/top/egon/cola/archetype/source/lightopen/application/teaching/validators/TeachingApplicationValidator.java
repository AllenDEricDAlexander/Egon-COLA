package top.egon.cola.archetype.source.lightopen.application.teaching.validators;

import top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.TeachingUseCaseException;
import top.egon.cola.archetype.source.lightopen.domain.teaching.client.CourseCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Lazy
@RequiredArgsConstructor
public class TeachingApplicationValidator {
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(5);

    @Qualifier("courseCachePort")
    private final CourseCachePort courseCachePort;

    public void validate(CreateSchoolClassCommand command) {
        requireText(command.name(), "INVALID_CLASS", "class name is required");
        requireText(command.semester(), "INVALID_SEMESTER", "semester is required");
        validateContext(command.operatorId(), command.idempotencyKey());
    }

    public void validate(CreateCourseCommand command) {
        requireText(command.code(), "INVALID_COURSE", "course code is required");
        requireText(command.name(), "INVALID_COURSE", "course name is required");
        validateContext(command.operatorId(), command.idempotencyKey());
    }

    public void validate(ScheduleCourseCommand command) {
        if (command.startsAt() == null || command.endsAt() == null
                || !command.startsAt().isBefore(command.endsAt())) {
            throw new TeachingUseCaseException("INVALID_SCHEDULE", "startsAt must be before endsAt");
        }
        validateContext(command.operatorId(), command.idempotencyKey());
    }

    private void validateContext(String operatorId, String idempotencyKey) {
        requireText(operatorId, "MISSING_OPERATOR", "operator context is required");
        requireText(idempotencyKey, "MISSING_IDEMPOTENCY_KEY", "idempotency key is required");
        if (!courseCachePort.claimIdempotency(idempotencyKey, IDEMPOTENCY_TTL)) {
            throw new TeachingUseCaseException("DUPLICATE_REQUEST", "request was already processed");
        }
    }

    private void requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new TeachingUseCaseException(code, message);
        }
    }
}
