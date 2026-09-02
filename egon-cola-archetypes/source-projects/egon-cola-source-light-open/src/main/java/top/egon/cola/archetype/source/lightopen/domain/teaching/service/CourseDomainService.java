package top.egon.cola.archetype.source.lightopen.domain.teaching.service;

import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseCode;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.Optional;

/** Persistence-owning course domain service contract. */
public interface CourseDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    Course createCourse(CourseCode code, String name);

    Course save(Course course);

    Optional<Course> findById(Long courseId);

    Optional<Course> findByCode(CourseCode courseCode);
}
