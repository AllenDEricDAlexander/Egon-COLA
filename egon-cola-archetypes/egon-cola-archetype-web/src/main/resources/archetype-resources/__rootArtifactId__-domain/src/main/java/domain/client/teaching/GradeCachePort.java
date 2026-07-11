package ${package}.domain.client.teaching;

import ${package}.domain.entities.teaching.Grade;

import java.util.Optional;

public interface GradeCachePort {
    Optional<Grade> findById(String gradeId);
    void put(Grade grade);
    void evict(String gradeId);
}
