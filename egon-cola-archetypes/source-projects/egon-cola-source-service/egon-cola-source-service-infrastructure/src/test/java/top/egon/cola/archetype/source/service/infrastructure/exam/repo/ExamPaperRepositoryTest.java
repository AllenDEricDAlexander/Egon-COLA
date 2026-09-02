package top.egon.cola.archetype.source.service.infrastructure.exam.repo;

import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.enums.ExamPaperStatus;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.service.infrastructure.exam.repo.converter.ExamPaperConverter;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamPaperRepositoryTest {
    @Test
    void shouldRoundTripPaper() {
        ExamPaper paper = new ExamPaper(5001L, new ExamId(4001L), "Paper", 100,
                ExamPaperStatus.DRAFT);
        ExamPaperConverter converter = Mappers.getMapper(ExamPaperConverter.class);
        var target = converter.toTarget(paper);
        target.setId(paper.getId());
        ExamPaper restored = converter.toSource(target);
        assertEquals(5001L, restored.getId());
        assertEquals(4001L, restored.getExamId().value());
    }
}
