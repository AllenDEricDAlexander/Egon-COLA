package ${package}.domain.teaching.repos;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.vos.GradeCode;

import java.util.Optional;

public interface GradeRepository {
    Optional<Grade> findById(String gradeId);
    Optional<Grade> findByCode(GradeCode code);
    boolean existsByCode(GradeCode code);
    Grade save(Grade grade);
}
