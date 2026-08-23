package ${package}.domain.teaching.vos;

public record SchoolClassId(long value) {
    public SchoolClassId {
        if (value <= 0) {
            throw new IllegalArgumentException("school class id must be positive");
        }
    }
}
