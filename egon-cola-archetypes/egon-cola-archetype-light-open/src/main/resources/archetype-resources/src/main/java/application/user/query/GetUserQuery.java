package ${package}.application.user.query;

public record GetUserQuery(long userId) {
    public GetUserQuery {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
