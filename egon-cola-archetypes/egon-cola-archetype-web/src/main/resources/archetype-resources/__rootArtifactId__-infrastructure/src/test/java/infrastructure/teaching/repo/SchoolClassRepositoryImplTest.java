package ${package}.infrastructure.teaching.repo;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.repos.GradeRepository;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.infrastructure.teaching.repo.converter.GradePOConverter;
import ${package}.infrastructure.teaching.repo.converter.GradePOMapperImpl;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.impl.GradeRepositoryImpl;
import ${package}.infrastructure.teaching.repo.impl.SchoolClassRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@Import({GradeRepositoryImpl.class, SchoolClassRepositoryImpl.class,
    GradePOConverter.class, GradePOMapperImpl.class, SchoolClassPOConverter.class})
@ContextConfiguration(classes = SchoolClassRepositoryImplTest.TestConfiguration.class)
class SchoolClassRepositoryImplTest {
    @Autowired GradeRepository gradeRepository;
    @Autowired SchoolClassRepository schoolClassRepository;

    @Test
    void savesClassAndChecksNameWithinGradeIgnoringCase() {
        Grade grade = gradeRepository.save(new Grade(
            "grade-1", GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE));
        schoolClassRepository.save(new SchoolClass(new SchoolClassId("class-1"), "Class A", grade.id(),
            grade.code(), grade.name(), SchoolClassStatus.ACTIVE, List.of()));

        assertThat(schoolClassRepository.findById(new SchoolClassId("class-1"))).isPresent();
        assertThat(schoolClassRepository.existsByGradeIdAndNameIgnoreCase("grade-1", "class a")).isTrue();
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackages = {
            "${package}.infrastructure.user.repo.po",
            "${package}.infrastructure.teaching.repo.po"
    })
    @EnableJpaRepositories(basePackages = {
            "${package}.infrastructure.user.repo.jpa",
            "${package}.infrastructure.teaching.repo.jpa"
    })
    static class TestConfiguration {}
}
