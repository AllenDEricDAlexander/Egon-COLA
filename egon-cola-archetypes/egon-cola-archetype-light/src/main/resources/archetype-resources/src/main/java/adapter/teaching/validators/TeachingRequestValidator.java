package ${package}.adapter.teaching.validators;

import ${package}.adapter.teaching.dto.ScheduleCourseRequest;
import ${package}.facade.teaching.dto.CreateCourseDTO;
import org.springframework.stereotype.Component;

@Component
public class TeachingRequestValidator {
    public void validateSchedule(ScheduleCourseRequest request) {
        if (request == null || request.startsAt() == null || request.endsAt() == null
                || !request.startsAt().isBefore(request.endsAt())) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
    }

    public void validate(CreateCourseDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("course import message must not be null");
        }
        requireText(request.code(), "code");
        requireText(request.name(), "name");
        requireText(request.operatorId(), "actor");
        requireText(request.requestId(), "messageId");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
