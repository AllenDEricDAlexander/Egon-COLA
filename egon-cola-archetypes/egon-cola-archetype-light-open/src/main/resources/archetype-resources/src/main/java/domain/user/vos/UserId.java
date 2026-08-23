package ${package}.domain.user.vos;

public record UserId(String value) {
    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("user id must not be blank");
        }
    }
}
