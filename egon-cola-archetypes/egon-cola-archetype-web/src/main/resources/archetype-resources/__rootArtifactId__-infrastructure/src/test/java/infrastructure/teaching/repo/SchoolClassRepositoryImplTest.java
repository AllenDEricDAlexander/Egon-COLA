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
import ${package}.infrastructure.teaching.repo.jpa.SchoolClassUserJpaRepository;
import ${package}.domain.user.vos.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration/default",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Import({GradeRepositoryImpl.class, SchoolClassRepositoryImpl.class,
    GradePOConverter.class, GradePOMapperImpl.class, SchoolClassPOConverter.class,
    UuidV7Generator.class})
@ContextConfiguration(classes = SchoolClassRepositoryImplTest.TestConfiguration.class)
class SchoolClassRepositoryImplTest {
    @Autowired GradeRepository gradeRepository;
    @Autowired SchoolClassRepository schoolClassRepository;
    @Autowired SchoolClassUserJpaRepository schoolClassUserJpaRepository;
    @Autowired ${package}.infrastructure.user.repo.jpa.UserJpaRepository userJpaRepository;

    @Test
    void savesClassAndChecksNameWithinGradeIgnoringCase() {
        UuidV7Generator idGenerator = new UuidV7Generator();
        String gradeId = idGenerator.nextId();
        String schoolClassId = idGenerator.nextId();
        String userId = idGenerator.nextId();
        Grade grade = gradeRepository.save(new Grade(
            gradeId, GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE));
        userJpaRepository.save(new ${package}.infrastructure.user.repo.po.UserPO(
            userId, "Mario", "mario@example.com", "ACTIVE", java.time.LocalDateTime.now()));
        schoolClassRepository.save(new SchoolClass(new SchoolClassId(schoolClassId), "Class A", grade.id(),
            grade.code(), grade.name(), SchoolClassStatus.ACTIVE, List.of()));

        assertThat(schoolClassRepository.findByGradeIdAndId(
                gradeId, new SchoolClassId(schoolClassId))).isPresent();
        assertThat(schoolClassRepository.existsByGradeIdAndNameIgnoreCase(gradeId, "class a")).isTrue();
        schoolClassRepository.addUser(
                gradeId, new SchoolClassId(schoolClassId), new UserId(userId));
        String relationId = schoolClassUserJpaRepository
                .findByGradeIdAndSchoolClassId(gradeId, schoolClassId)
                .getFirst()
                .getId();
        assertThat(relationId).hasSize(36);
        assertThat(UUID.fromString(relationId).version()).isEqualTo(7);
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
