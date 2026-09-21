package top.egon.cola.archetype.source.lightopen.adapter.user.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.lightopen.adapter.user.pojo.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.lightopen.adapter.user.pojo.vo.UserDetailVO;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.PermissionDetailResult;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.UserResult;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

import java.util.List;
import java.util.Objects;

/** User use-case result to view projection. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, imports = List.class)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "userAdapterConvertorImpl"))
public interface UserAdapterConvertor extends BaseForwardConverter<UserResult, UserDetailVO> {

    @Override
    UserDetailVO toTarget(UserResult source);

    List<PermissionTreeVO> toPermissionTree(List<PermissionDetailResult> results);

    @Mapping(target = "children", expression = "java(List.of())")
    PermissionTreeVO toPermissionTreeItem(PermissionDetailResult result);

    @BeforeMapping
    default void requireUserResult(UserResult result) {
        Objects.requireNonNull(result, "result");
    }

    @BeforeMapping
    default void requirePermissionResults(List<PermissionDetailResult> results) {
        Objects.requireNonNull(results, "results");
    }

    @BeforeMapping
    default void requirePermissionResult(PermissionDetailResult result) {
        Objects.requireNonNull(result, "result");
    }
}
