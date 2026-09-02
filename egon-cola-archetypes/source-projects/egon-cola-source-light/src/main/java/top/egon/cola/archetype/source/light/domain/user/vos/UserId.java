package top.egon.cola.archetype.source.light.domain.user.vos;

public record UserId(Long value) {
    public UserId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("user id must be positive");
        }
    }

}
