package top.egon.cola.archetype.source.web.adapter.teaching.controller;

import top.egon.cola.archetype.source.web.adapter.teaching.converter.SchoolClassAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.teaching.dto.AssignUserToClassRequest;
import top.egon.cola.archetype.source.web.adapter.teaching.dto.CreateSchoolClassRequest;
import top.egon.cola.archetype.source.web.adapter.teaching.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.web.application.teaching.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
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
import java.util.UUID;

@RestController("schoolClassController")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SchoolClassController {
    private final SchoolClassManage schoolClassManage;
    private final SchoolClassAdapterConverter converter;

    @PostMapping("/school-classes")
    public ResponseEntity<SchoolClassDetailVO> create(
            @Valid @RequestBody CreateSchoolClassRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null ? UUID.randomUUID().toString() : key;
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
                        OrganizationFacadeSupport.positiveId(gradeId, "gradeId"),
                        OrganizationFacadeSupport.positiveId(schoolClassId, "schoolClassId"))));
    }

    @PostMapping("/grades/{gradeId}/school-classes/{schoolClassId}/users")
    public ResponseEntity<Void> assignUser(
            @PathVariable String gradeId,
            @PathVariable String schoolClassId,
            @Valid @RequestBody AssignUserToClassRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        schoolClassManage.assignUser(
                converter.toCommand(requestId,
                        OrganizationFacadeSupport.positiveId(gradeId, "gradeId"),
                        OrganizationFacadeSupport.positiveId(schoolClassId, "schoolClassId"),
                        OrganizationFacadeSupport.positiveId(request.userId(), "userId")));
        return ResponseEntity.noContent().build();
    }
}
