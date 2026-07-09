package ${package}.application.user.query;

public record GetUserQuery(String userId) {
    public GetUserQuery {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }
}
