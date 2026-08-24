package ${package}.infrastructure.teaching.repo;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.impl.SchoolClassRepositoryImpl;
import ${package}.infrastructure.teaching.repo.mapper.GradeMapper;
import ${package}.infrastructure.teaching.repo.mapper.SchoolClassMapper;
import ${package}.infrastructure.teaching.repo.mapper.SchoolClassUserMapper;
import ${package}.infrastructure.teaching.repo.po.GradePO;
import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import ${package}.infrastructure.teaching.repo.po.SchoolClassUserPO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchoolClassRepositoryImplTest {

    @Test
    void preservesGradeRouteForClassAndMembershipMapperCalls() {
        SchoolClassMapper schoolClassMapper = mock(SchoolClassMapper.class);
        GradeMapper gradeMapper = mock(GradeMapper.class);
        SchoolClassUserMapper membershipMapper = mock(SchoolClassUserMapper.class);
        GradePO grade = new GradePO(
                1001L, "GRADE_ONE", "Grade One", "ACTIVE", LocalDateTime.now());
        SchoolClassPO schoolClass = new SchoolClassPO(
                2001L, "Class A", "Grade One", 1001L, "ACTIVE", LocalDateTime.now());
        when(schoolClassMapper.selectByGradeIdAndId(1001L, 2001L))
                .thenReturn(null, schoolClass);
        when(schoolClassMapper.insert(any(SchoolClassPO.class))).thenReturn(1);
        when(gradeMapper.selectById(1001L)).thenReturn(grade);
        when(membershipMapper.selectByGradeIdAndSchoolClassId(1001L, 2001L)).thenReturn(
                List.of(new SchoolClassUserPO(9001L, 1001L, 2001L, 3001L, LocalDateTime.now())));
        when(membershipMapper.insert(any(SchoolClassUserPO.class))).thenReturn(1);
        when(membershipMapper.countByGradeIdAndSchoolClassIdAndUserId(1001L, 2001L, 3001L))
                .thenReturn(1L);

        SchoolClassRepositoryImpl repository = new SchoolClassRepositoryImpl(
                schoolClassMapper,
                gradeMapper,
                membershipMapper,
                new SchoolClassPOConverter(),
                (LongIdGenerator) () -> 9001L);

        SchoolClass saved = repository.save(new SchoolClass(
                new SchoolClassId(2001L), "Class A", 1001L, GradeCode.create("GRADE_ONE"),
                "Grade One", SchoolClassStatus.ACTIVE, List.of()));

        assertThat(saved.id().value()).isEqualTo(2001L);
        assertThat(repository.findByGradeIdAndId(1001L, new SchoolClassId(2001L))).isPresent();
        assertThat(repository.existsByGradeIdAndNameIgnoreCase(1001L, "class a")).isFalse();
        repository.addUser(1001L, new SchoolClassId(2001L), new UserId(3001L));
        assertThat(repository.hasUser(1001L, new SchoolClassId(2001L), new UserId(3001L)))
                .isTrue();
    }
}
