package ${package}.adapter.user.convertor;

import ${package}.adapter.user.vo.PermissionTreeVO;
import ${package}.adapter.user.vo.UserDetailVO;
import ${package}.application.user.result.PermissionDetailResult;
import ${package}.application.user.result.UserResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = List.class)
public interface UserAdapterConvertor {

    UserDetailVO toUserDetail(UserResult result);

    List<PermissionTreeVO> toPermissionTree(List<PermissionDetailResult> results);

    @Mapping(target = "children", expression = "java(List.of())")
    PermissionTreeVO toPermissionTreeItem(PermissionDetailResult result);
}
