#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.converter;

import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.result.ScoreResult;
import ${package}.application.result.PageResult;
import ${package}.facade.evaluation.v1.PageScoreResponse;
import ${package}.facade.evaluation.v1.Score;
import ${package}.facade.evaluation.v1.RecordScoreRequest;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ScoreFacadeConverter {

    public RecordScoreCommand toCommand(RecordScoreRequest request) {
        Objects.requireNonNull(request, "request");
        return new RecordScoreCommand(request.getExamId(), request.getStudentId(), request.getPoints());
    }

    public Score toResponse(ScoreResult result) {
        Objects.requireNonNull(result, "result");
        return Score.newBuilder()
                .setId(result.id())
                .setExamId(result.examId())
                .setCourseId(result.courseId())
                .setStudentId(result.studentId())
                .setPoints(result.points())
                .setStatus(result.status())
                .build();
    }

    public PageScoreResponse toPage(PageResult<ScoreResult> page) {
        Objects.requireNonNull(page, "page");
        PageScoreResponse.Builder builder = PageScoreResponse.newBuilder()
                .setCurrentPage(page.currentPage())
                .setTotalPages(page.totalPages())
                .setPageSize(page.pageSize())
                .setTotalCount(page.totalCount());
        page.records().stream().map(this::toResponse).forEach(builder::addRecords);
        return builder.build();
    }
}
