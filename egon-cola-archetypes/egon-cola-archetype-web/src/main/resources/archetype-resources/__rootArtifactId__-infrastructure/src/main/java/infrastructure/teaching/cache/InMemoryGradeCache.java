package ${package}.infrastructure.teaching.cache;

import ${package}.domain.teaching.client.GradeCachePort;
import ${package}.domain.teaching.entities.Grade;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryGradeCache implements GradeCachePort {
    private final ConcurrentHashMap<String, Grade> values = new ConcurrentHashMap<>();
    @Override public Optional<Grade> findById(String id) { return Optional.ofNullable(values.get(id)); }
    @Override public void put(Grade grade) { values.put(grade.id(), grade); }
    @Override public void evict(String id) { values.remove(id); }
}
