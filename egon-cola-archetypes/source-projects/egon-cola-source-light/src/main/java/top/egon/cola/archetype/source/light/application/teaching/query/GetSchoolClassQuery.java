package top.egon.cola.archetype.source.light.application.teaching.query;

public record GetSchoolClassQuery(Long schoolClassId) {
    public GetSchoolClassQuery {
        if (schoolClassId == null || schoolClassId <= 0) {
            throw new IllegalArgumentException("schoolClassId must be positive");
        }
    }

}
