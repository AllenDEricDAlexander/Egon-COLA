package top.egon.cola.archetype.source.web.adapter.user.converter;

import top.egon.cola.archetype.source.web.adapter.user.dto.GrantPermissionRequest;
import top.egon.cola.archetype.source.web.adapter.user.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.web.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.web.application.user.result.PermissionTreeResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PermissionAdapterConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "request.permissionCode")
    GrantPermissionCommand toCommand(
            String requestId,
            String roleCode,
            GrantPermissionRequest request);

    PermissionTreeVO toVO(PermissionTreeResult result);

    @BeforeMapping
    default void requireRequest(GrantPermissionRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(PermissionTreeResult result) {
        Objects.requireNonNull(result, "result");
    }
}
