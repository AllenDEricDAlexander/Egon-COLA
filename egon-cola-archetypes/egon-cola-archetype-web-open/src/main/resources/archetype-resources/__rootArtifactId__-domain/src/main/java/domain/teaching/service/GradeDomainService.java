package ${package}.domain.teaching.service;

import ${package}.domain.teaching.entities.Grade;

public interface GradeDomainService {
    Grade create(Long gradeId, String code, String name);
}
