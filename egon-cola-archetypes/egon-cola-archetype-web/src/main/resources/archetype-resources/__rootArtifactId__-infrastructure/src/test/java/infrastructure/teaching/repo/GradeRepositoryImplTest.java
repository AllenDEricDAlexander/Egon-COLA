package ${package}.infrastructure.teaching.repo;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.repos.GradeRepository;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.infrastructure.teaching.repo.converter.GradePOConverter;
import ${package}.infrastructure.teaching.repo.converter.GradePOMapper;
import ${package}.infrastructure.teaching.repo.converter.GradePOMapperImpl;
import ${package}.infrastructure.teaching.repo.impl.GradeRepositoryImpl;
import ${package}.infrastructure.teaching.repo.jpa.GradeJpaRepository;
import ${package}.infrastructure.teaching.repo.po.GradePO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration/default",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Import({GradeRepositoryImpl.class, GradePOConverter.class, GradePOMapperImpl.class})
@ContextConfiguration(classes = GradeRepositoryImplTest.TestConfiguration.class)
class GradeRepositoryImplTest {
    @Autowired GradeRepository repository;
    @Autowired GradeJpaRepository jpaRepository;
    @Autowired GradePOMapper gradePOMapper;

    @Test
    void savesAndRestoresNormalizedGrades() {
        String gradeId = new UuidV7Generator().nextId();
        Grade saved = repository.save(new Grade(
            gradeId, GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE));

        assertThat(repository.findByCode(new GradeCode("GRADE_ONE"))).contains(saved);
        assertThat(repository.findById(gradeId)).contains(saved);
    }

    @Test
    void updatesTargetWhenMappingGrade() {
        GradePO target = new GradePO(
            "old", "OLD", "Old", "INACTIVE", LocalDateTime.MIN);

        String gradeId = new UuidV7Generator().nextId();
        GradePO mapped = gradePOMapper.convert(new Grade(
            gradeId, GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE), target);

        assertThat(mapped).isSameAs(target);
        assertThat(mapped.getId()).isEqualTo(gradeId);
        assertThat(mapped.getCode()).isEqualTo("GRADE_ONE");
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
