package ${package}.domain.teaching.repos;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;

import java.util.Optional;

public interface SchoolClassRepository {
    SchoolClass save(SchoolClass schoolClass);
    Optional<SchoolClass> findByGradeIdAndId(
            Long gradeId,
            SchoolClassId schoolClassId);
    boolean existsByGradeIdAndNameIgnoreCase(Long gradeId, String name);
    void addUser(Long gradeId, SchoolClassId schoolClassId, UserId userId);
    boolean hasUser(Long gradeId, SchoolClassId schoolClassId, UserId userId);
}
