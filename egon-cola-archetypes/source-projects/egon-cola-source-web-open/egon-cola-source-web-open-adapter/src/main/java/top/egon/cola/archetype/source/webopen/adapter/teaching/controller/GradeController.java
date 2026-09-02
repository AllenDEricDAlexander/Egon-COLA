package top.egon.cola.archetype.source.webopen.adapter.teaching.controller;

import top.egon.cola.archetype.source.webopen.adapter.teaching.converter.GradeAdapterConverter;
import top.egon.cola.archetype.source.webopen.adapter.teaching.dto.CreateGradeRequest;
import top.egon.cola.archetype.source.webopen.adapter.teaching.vo.GradeDetailVO;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.webopen.application.teaching.query.GradeDetailQuery;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationIdBoundary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController("gradeController")
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
public class GradeController {
    private final GradeManage gradeManage;
    private final GradeAdapterConverter converter;
    private final LongIdGenerator idGenerator;

    @PostMapping
    public ResponseEntity<GradeDetailVO> create(
            @Valid @RequestBody CreateGradeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null || key.isBlank() ? Long.toString(idGenerator.nextLongId()) : key;
        GradeDetailVO result = converter.toVO(gradeManage.createGrade(converter.toCommand(requestId, request)));
        return ResponseEntity.created(URI.create("/api/v1/grades/" + result.id())).body(result);
    }

    @GetMapping("/{gradeId}")
    public GradeDetailVO get(@PathVariable String gradeId) {
        return converter.toVO(gradeManage.getGrade(new GradeDetailQuery(
            OrganizationIdBoundary.parse(gradeId, "gradeId"))));
    }
}
