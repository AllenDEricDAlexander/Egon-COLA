package ${package}.infrastructure.teaching.repo;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.infrastructure.teaching.repo.converter.GradePOConverter;
import ${package}.infrastructure.teaching.repo.converter.GradePOMapper;
import ${package}.infrastructure.teaching.repo.impl.GradeRepositoryImpl;
import ${package}.infrastructure.teaching.repo.mapper.GradeMapper;
import ${package}.infrastructure.teaching.repo.po.GradePO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GradeRepositoryImplTest {

    @Test
    void savesAndRestoresGradeThroughMapper() {
        GradeMapper gradeMapper = mock(GradeMapper.class);
        GradePOMapper poMapper = mock(GradePOMapper.class);
        GradePO row = new GradePO(
                1001L, "GRADE_ONE", "Grade One", "ACTIVE", LocalDateTime.now());
        when(gradeMapper.selectById(1001L)).thenReturn(null, row);
        when(gradeMapper.selectByCode("GRADE_ONE")).thenReturn(row);
        when(gradeMapper.insert(any(GradePO.class))).thenReturn(1);
        when(poMapper.convert(any(Grade.class))).thenReturn(row);

        GradeRepositoryImpl repository = new GradeRepositoryImpl(
                gradeMapper, new GradePOConverter(poMapper));

        Grade saved = repository.save(new Grade(
                1001L, GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE));

        assertThat(repository.findByCode(new GradeCode("GRADE_ONE"))).contains(saved);
        assertThat(repository.findById(1001L)).contains(saved);
        assertThat(repository.existsByCode(new GradeCode("GRADE_ONE"))).isFalse();
    }
}
