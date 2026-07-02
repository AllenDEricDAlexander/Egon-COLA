package top.egon.fable-web.domain.repos.teaching;

import top.egon.fable-web.domain.entities.teaching.SchoolClass;

import java.util.Optional;

public interface SchoolClassRepository {
    SchoolClass save(SchoolClass schoolClass);
    Optional<SchoolClass> findById(String schoolClassId);
}
