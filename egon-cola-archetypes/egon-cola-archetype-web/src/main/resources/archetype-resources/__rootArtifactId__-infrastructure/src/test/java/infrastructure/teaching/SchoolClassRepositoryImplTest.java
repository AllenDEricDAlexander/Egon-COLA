package ${package}.infrastructure.teaching;

import ${package}.domain.entities.teaching.Grade;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.enums.teaching.GradeStatus;
import ${package}.domain.enums.teaching.SchoolClassStatus;
import ${package}.domain.repos.teaching.GradeRepository;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import ${package}.domain.vos.teaching.GradeCode;
import ${package}.domain.vos.teaching.SchoolClassId;
import ${package}.infrastructure.repo.teaching.converter.GradePOConverter;
import ${package}.infrastructure.repo.teaching.converter.SchoolClassPOConverter;
import ${package}.infrastructure.repo.teaching.impl.GradeRepositoryImpl;
import ${package}.infrastructure.repo.teaching.impl.SchoolClassRepositoryImpl;
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
    GradePOConverter.class, SchoolClassPOConverter.class})
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
    @EntityScan(basePackages = "${package}.infrastructure.repo")
    @EnableJpaRepositories(basePackages = "${package}.infrastructure.repo")
    static class TestConfiguration {}
}
