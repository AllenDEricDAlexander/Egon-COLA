package ${package}.adapter.controller.teaching;

import ${package}.adapter.convertor.SchoolClassAdapterConverter;
import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.common.response.Response;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.teaching.CreateSchoolClassRequest;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/school-classes")
public class SchoolClassController {
    private final SchoolClassManage schoolClassManage;

    public SchoolClassController(SchoolClassManage schoolClassManage) {
        this.schoolClassManage = schoolClassManage;
    }

    @PostMapping
    public SingleResponse<SchoolClassDTO> create(@Valid @RequestBody CreateSchoolClassRequest request) {
        return SingleResponse.of(SchoolClassAdapterConverter.toDto(schoolClassManage.create(request.name(), request.gradeName())));
    }

    @PostMapping("/{schoolClassId}/users/{userId}")
    public Response assignUser(@PathVariable String schoolClassId, @PathVariable String userId) {
        schoolClassManage.assignUser(userId, schoolClassId);
        return Response.success();
    }
}
