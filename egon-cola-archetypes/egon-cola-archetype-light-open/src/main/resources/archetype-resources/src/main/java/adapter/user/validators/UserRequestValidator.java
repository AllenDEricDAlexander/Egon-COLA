package ${package}.adapter.user.validators;

import ${package}.facade.user.dto.CreateUserDTO;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class UserRequestValidator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public void validate(CreateUserDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("user import message must not be null");
        }
        requireText(request.externalId(), "externalId");
        requireText(request.name(), "name");
        requireText(request.operatorId(), "actor");
        requireText(request.requestId(), "messageId");
        if (request.email() == null || !EMAIL.matcher(request.email()).matches()) {
            throw new IllegalArgumentException("email must be valid");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
