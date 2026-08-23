package ${package}.domain.user.vos;

public record ExternalUser(String externalId, String name) {
    public ExternalUser {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("external id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
