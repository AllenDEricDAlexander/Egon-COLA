package top.egon.cola.archetype.source.web.domain.teaching.client;

import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;

import java.util.Optional;

public interface GradeCachePort {
    Optional<Grade> findById(Long gradeId);
    void put(Grade grade);
    void evict(Long gradeId);
}
