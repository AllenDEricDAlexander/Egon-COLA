package ${package}.domain.teaching.repos;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;

import java.util.Optional;

public interface SchoolClassRepository {
    SchoolClass save(SchoolClass schoolClass);
    Optional<SchoolClass> findByGradeIdAndId(
            String gradeId,
            SchoolClassId schoolClassId);
    boolean existsByGradeIdAndNameIgnoreCase(String gradeId, String name);
    void addUser(String gradeId, SchoolClassId schoolClassId, UserId userId);
    boolean hasUser(String gradeId, SchoolClassId schoolClassId, UserId userId);
}
