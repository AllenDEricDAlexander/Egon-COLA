package top.egon.cola.archetype.source.lightopen.domain.teaching.vos;

public record SchoolClassId(Long value) {
    public SchoolClassId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("school class id must be positive");
        }
    }

}
