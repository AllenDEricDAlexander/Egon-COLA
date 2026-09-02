package top.egon.cola.archetype.source.lightopen.application.user.query;

public record GetUserPermissionsQuery(Long userId) {
    public GetUserPermissionsQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }

}
