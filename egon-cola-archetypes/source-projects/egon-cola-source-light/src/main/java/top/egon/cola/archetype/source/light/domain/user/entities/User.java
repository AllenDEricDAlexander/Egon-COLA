package top.egon.cola.archetype.source.light.domain.user.entities;

import top.egon.cola.archetype.source.light.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.light.domain.user.vos.UserId;

import java.util.Objects;

public final class User {
    private final UserId id;
    private final String externalId;
    private final String name;
    private final String email;
    private final UserStatus status;

    public User(UserId id, String name, String email, UserStatus status) {
        this(id, String.valueOf(id.value()), name, email, status);
    }

    public User(UserId id, String externalId, String name, String email, UserStatus status) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.externalId = requireText(externalId, "externalId");
        this.name = requireText(name, "name");
        this.email = requireText(email, "email");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public UserId id() {
        return id;
    }

    public String externalId() {
        return externalId;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public UserStatus status() {
        return status;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
