package ${package}.domain.teaching.service;

import ${package}.domain.teaching.entities.Grade;

public interface GradeDomainService {
    Grade create(String gradeId, String code, String name);
}
