package ${package}.domain.repos.teaching;

import ${package}.domain.entities.teaching.SchoolClass;

import java.util.Optional;

public interface SchoolClassRepository {
    SchoolClass save(SchoolClass schoolClass);
    Optional<SchoolClass> findById(String schoolClassId);
}
