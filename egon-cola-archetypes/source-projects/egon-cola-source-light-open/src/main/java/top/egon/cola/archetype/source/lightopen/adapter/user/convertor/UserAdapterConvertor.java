package top.egon.cola.archetype.source.lightopen.adapter.user.convertor;

import top.egon.cola.archetype.source.lightopen.adapter.user.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.lightopen.adapter.user.vo.UserDetailVO;
import top.egon.cola.archetype.source.lightopen.application.user.result.PermissionDetailResult;
import top.egon.cola.archetype.source.lightopen.application.user.result.UserResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = List.class)
public interface UserAdapterConvertor {

    UserDetailVO toUserDetail(UserResult result);

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
