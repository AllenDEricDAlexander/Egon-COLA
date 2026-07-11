package ${package}.domain.service.teaching;

import ${package}.domain.entities.teaching.Grade;

public interface GradeDomainService {
    Grade create(String gradeId, String code, String name);
}
