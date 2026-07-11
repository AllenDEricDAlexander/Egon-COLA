package ${package}.domain.repos.teaching;

import ${package}.domain.entities.teaching.Grade;
import ${package}.domain.vos.teaching.GradeCode;

import java.util.Optional;

public interface GradeRepository {
    Optional<Grade> findById(String gradeId);
    Optional<Grade> findByCode(GradeCode code);
    boolean existsByCode(GradeCode code);
    Grade save(Grade grade);
}
