package top.egon.cola.archetype.source.webopen.adapter.user.pojo.convertor;

import top.egon.cola.archetype.source.webopen.adapter.user.pojo.dto.GrantPermissionRequest;
import top.egon.cola.archetype.source.webopen.adapter.user.pojo.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.webopen.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.webopen.application.user.pojo.result.PermissionTreeResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "permissionAdapterConverterImpl"))
public interface PermissionAdapterConverter extends BaseForwardConverter<PermissionTreeResult, PermissionTreeVO> {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "request.permissionCode")
    GrantPermissionCommand toCommand(
            String requestId,
            String roleCode,
            GrantPermissionRequest request);

    @Override
    PermissionTreeVO toTarget(PermissionTreeResult source);

    /** Callable contract kept from the previous hand-written projection. */
    default PermissionTreeVO toVO(PermissionTreeResult result) {
        return toTarget(result);
    }

    @BeforeMapping
    default void requireRequest(GrantPermissionRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(PermissionTreeResult result) {
        Objects.requireNonNull(result, "result");
    }
}
