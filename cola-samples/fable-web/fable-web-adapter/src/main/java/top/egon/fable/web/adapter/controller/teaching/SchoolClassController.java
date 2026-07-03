package top.egon.fable.web.adapter.controller.teaching;

import top.egon.fable.web.adapter.convertor.SchoolClassAdapterConverter;
import top.egon.fable.web.application.manage.teaching.SchoolClassManage;
import top.egon.fable.web.common.response.Response;
import top.egon.fable.web.common.response.SingleResponse;
import top.egon.fable.web.facade.dto.teaching.CreateSchoolClassRequest;
import top.egon.fable.web.facade.dto.teaching.SchoolClassDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("schoolClassController")
@RequestMapping("/school-classes")
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
