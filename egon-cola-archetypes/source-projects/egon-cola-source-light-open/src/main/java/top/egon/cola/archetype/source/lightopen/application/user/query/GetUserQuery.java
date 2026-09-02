package top.egon.cola.archetype.source.lightopen.application.user.query;

public record GetUserQuery(Long userId) {
    public GetUserQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }

}
