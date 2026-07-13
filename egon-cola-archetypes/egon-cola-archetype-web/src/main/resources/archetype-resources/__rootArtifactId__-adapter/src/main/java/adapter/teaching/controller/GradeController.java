package ${package}.adapter.teaching.controller;

import ${package}.adapter.teaching.converter.GradeAdapterConverter;
import ${package}.adapter.teaching.dto.CreateGradeRequest;
import ${package}.adapter.teaching.vo.GradeDetailVO;
import ${package}.application.teaching.manage.GradeManage;
import ${package}.application.teaching.query.GradeDetailQuery;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController("gradeController")
@RequestMapping("/api/v1/grades")
public class GradeController {
    private final GradeManage gradeManage;
    private final GradeAdapterConverter converter;

    public GradeController(GradeManage gradeManage, GradeAdapterConverter converter) {
        this.gradeManage = gradeManage;
        this.converter = converter;
    }

    @PostMapping
    public ResponseEntity<GradeDetailVO> create(
            @Valid @RequestBody CreateGradeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        GradeDetailVO result = converter.toVO(gradeManage.createGrade(converter.toCommand(requestId, request)));
        return ResponseEntity.created(URI.create("/api/v1/grades/" + result.id())).body(result);
    }

    @GetMapping("/{gradeId}")
    public GradeDetailVO get(@PathVariable String gradeId) {
        return converter.toVO(gradeManage.getGrade(new GradeDetailQuery(gradeId)));
    }
}
