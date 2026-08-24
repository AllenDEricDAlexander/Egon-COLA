package ${package}.adapter.teaching.controller;

import ${package}.adapter.teaching.converter.SchoolClassAdapterConverter;
import ${package}.adapter.teaching.dto.AssignUserToClassRequest;
import ${package}.adapter.teaching.dto.CreateSchoolClassRequest;
import ${package}.adapter.teaching.vo.SchoolClassDetailVO;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.query.SchoolClassDetailQuery;
import ${package}.adapter.facade.impl.OrganizationIdBoundary;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController("schoolClassController")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SchoolClassController {
    private final SchoolClassManage schoolClassManage;
    private final SchoolClassAdapterConverter converter;
    private final LongIdGenerator idGenerator;

    @PostMapping("/school-classes")
    public ResponseEntity<SchoolClassDetailVO> create(
            @Valid @RequestBody CreateSchoolClassRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null || key.isBlank() ? Long.toString(idGenerator.nextLongId()) : key;
        SchoolClassDetailVO result = converter.toVO(
            schoolClassManage.createSchoolClass(converter.toCommand(requestId, request)));
        return ResponseEntity.created(URI.create("/api/v1/school-classes/" + result.id())).body(result);
    }

    @GetMapping("/grades/{gradeId}/school-classes/{schoolClassId}")
    public SchoolClassDetailVO get(
            @PathVariable String gradeId,
            @PathVariable String schoolClassId) {
        return converter.toVO(
                schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(
                    OrganizationIdBoundary.parse(gradeId, "gradeId"),
                    OrganizationIdBoundary.parse(schoolClassId, "schoolClassId"))));
    }

    @PostMapping("/grades/{gradeId}/school-classes/{schoolClassId}/users")
    public ResponseEntity<Void> assignUser(
            @PathVariable String gradeId,
            @PathVariable String schoolClassId,
            @Valid @RequestBody AssignUserToClassRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null || key.isBlank() ? Long.toString(idGenerator.nextLongId()) : key;
        schoolClassManage.assignUser(
                converter.toCommand(requestId,
                    OrganizationIdBoundary.parse(gradeId, "gradeId"),
                    OrganizationIdBoundary.parse(schoolClassId, "schoolClassId"),
                    OrganizationIdBoundary.parse(request.userId(), "userId")));
        return ResponseEntity.noContent().build();
    }
}
