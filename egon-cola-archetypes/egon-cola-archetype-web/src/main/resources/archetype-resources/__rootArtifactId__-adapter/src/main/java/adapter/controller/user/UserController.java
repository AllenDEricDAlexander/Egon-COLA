package ${package}.adapter.controller.user;

import ${package}.adapter.convertor.UserAdapterConverter;
import ${package}.application.manage.user.UserManage;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.user.CreateUserRequest;
import ${package}.facade.dto.user.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("userController")
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserController {
    @Qualifier("userManage")
    private final UserManage userManage;

    @Qualifier("userAdapterConverter")
    private final UserAdapterConverter userAdapterConverter;

    @PostMapping
    public SingleResponse<UserDTO> create(@Valid @RequestBody CreateUserRequest request) {
        return SingleResponse.of(userAdapterConverter.toDto(userManage.create(request.name(), request.email())));
    }

    @GetMapping("/{userId}")
    public SingleResponse<UserDTO> getById(@PathVariable String userId) {
        return SingleResponse.of(userAdapterConverter.toDto(userManage.getById(userId)));
    }

    @GetMapping
    public SingleResponse<PageResponse<UserDTO>> getPage(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize) {
        return SingleResponse.of(userAdapterConverter.toPageResponse(userManage.getPage(currentPage, pageSize)));
    }
}
