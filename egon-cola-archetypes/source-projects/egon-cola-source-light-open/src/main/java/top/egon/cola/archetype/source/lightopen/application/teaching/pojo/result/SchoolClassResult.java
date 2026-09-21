package top.egon.cola.archetype.source.lightopen.application.teaching.pojo.result;

public record SchoolClassResult(
        Long id,
        String name,
        String semester,
        String status,
        int scheduleCount) {
}
