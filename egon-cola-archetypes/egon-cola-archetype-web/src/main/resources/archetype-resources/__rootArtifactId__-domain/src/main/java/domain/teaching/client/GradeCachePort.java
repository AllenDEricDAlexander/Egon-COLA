package ${package}.domain.teaching.client;

import ${package}.domain.teaching.entities.Grade;

import java.util.Optional;

public interface GradeCachePort {
    Optional<Grade> findById(Long gradeId);
    void put(Grade grade);
    void evict(Long gradeId);
}
