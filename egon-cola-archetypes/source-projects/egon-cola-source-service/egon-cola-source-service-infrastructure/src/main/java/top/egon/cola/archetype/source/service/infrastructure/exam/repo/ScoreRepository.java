package top.egon.cola.archetype.source.service.infrastructure.exam.repo;

import top.egon.cola.archetype.source.service.infrastructure.exam.dao.ScoreDAO;
import top.egon.cola.archetype.source.service.infrastructure.exam.po.ScorePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import java.util.Collection;
import java.util.List;
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;

/** Logical persistence boundary; query implementations remain explicit Mapper SQL. */
@Slf4j
@Validated
@Repository("scoreRepository")
@RequiredArgsConstructor
public class ScoreRepository extends EgonColaRepository<ScoreDAO, ScorePO> {
    @Getter
    @Qualifier("scoreDAO")
    private final ScoreDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public ScorePO selectByExamIdAndId(@Positive Long examId, @Positive Long id) {
        return getBaseMapper().selectByExamIdAndId(examId, id);
    }

    public long countByExamIdAndStudentId(@Positive Long examId, @Positive Long studentId) {
        return getBaseMapper().countByExamIdAndStudentId(examId, studentId);
    }

    public List<ScorePO> selectPageByExamId(@Positive Long examId, @Positive @Max(500) int limit, @PositiveOrZero int offset) {
        return getBaseMapper().selectPageByExamId(examId, limit, offset);
    }

    public long countByExamId(@Positive Long examId) {
        return getBaseMapper().countByExamId(examId);
    }
}
