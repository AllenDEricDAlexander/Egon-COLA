package ${package}.adapter.controller.teaching;

import ${package}.adapter.converter.SchoolClassAdapterConverter;
import ${package}.adapter.dto.teaching.AssignUserToClassRequest;
import ${package}.adapter.dto.teaching.CreateSchoolClassRequest;
import ${package}.adapter.vo.teaching.SchoolClassDetailVO;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.query.SchoolClassDetailQuery;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/school-classes")
public class SchoolClassController {
    private final SchoolClassManage schoolClassManage;
    private final SchoolClassAdapterConverter converter;

    public SchoolClassController(SchoolClassManage schoolClassManage, SchoolClassAdapterConverter converter) {
        this.schoolClassManage = schoolClassManage;
        this.converter = converter;
    }

    @PostMapping
    public ResponseEntity<SchoolClassDetailVO> create(
            @Valid @RequestBody CreateSchoolClassRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        SchoolClassDetailVO result = converter.toVO(
            schoolClassManage.createSchoolClass(converter.toCommand(requestId, request)));
        return ResponseEntity.created(URI.create("/api/v1/school-classes/" + result.id())).body(result);
    }

    @GetMapping("/{schoolClassId}")
    public SchoolClassDetailVO get(@PathVariable String schoolClassId) {
        return converter.toVO(schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(schoolClassId)));
    }

    @PostMapping("/{schoolClassId}/users")
    public ResponseEntity<Void> assignUser(
            @PathVariable String schoolClassId,
            @Valid @RequestBody AssignUserToClassRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        schoolClassManage.assignUser(converter.toCommand(requestId, schoolClassId, request.userId()));
        return ResponseEntity.noContent().build();
    }
}
