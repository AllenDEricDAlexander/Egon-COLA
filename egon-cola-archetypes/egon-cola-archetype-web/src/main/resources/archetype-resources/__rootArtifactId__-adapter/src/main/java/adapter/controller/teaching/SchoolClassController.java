package ${package}.adapter.controller.teaching;

import ${package}.adapter.convertor.SchoolClassAdapterConverter;
import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.common.response.Response;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.teaching.CreateSchoolClassRequest;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("schoolClassController")
@RequestMapping("/school-classes")
@Validated
@RequiredArgsConstructor
public class SchoolClassController {
    @Qualifier("schoolClassManage")
    private final SchoolClassManage schoolClassManage;

    @Qualifier("schoolClassAdapterConverter")
    private final SchoolClassAdapterConverter schoolClassAdapterConverter;

    @PostMapping
    public SingleResponse<SchoolClassDTO> create(@Valid @RequestBody CreateSchoolClassRequest request) {
        return SingleResponse.of(schoolClassAdapterConverter.toDto(schoolClassManage.create(request.name(), request.gradeName())));
    }

    @PostMapping("/{schoolClassId}/users/{userId}")
    public Response assignUser(@PathVariable String schoolClassId, @PathVariable String userId) {
        schoolClassManage.assignUser(userId, schoolClassId);
        return Response.success();
    }
}
