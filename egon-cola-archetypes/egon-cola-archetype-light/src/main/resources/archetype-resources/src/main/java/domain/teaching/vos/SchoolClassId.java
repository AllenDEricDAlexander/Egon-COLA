package ${package}.domain.teaching.vos;

public record SchoolClassId(String value) {
    public SchoolClassId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("school class id must not be blank");
        }
    }
}
