package top.egon.fable-web.adapter.controller.user;

import top.egon.fable-web.adapter.convertor.UserAdapterConverter;
import top.egon.fable-web.application.manage.user.UserManage;
import top.egon.fable-web.common.response.SingleResponse;
import top.egon.fable-web.facade.dto.user.CreateUserRequest;
import top.egon.fable-web.facade.dto.user.UserDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserManage userManage;

    public UserController(UserManage userManage) {
        this.userManage = userManage;
    }

    @PostMapping
    public SingleResponse<UserDTO> create(@Valid @RequestBody CreateUserRequest request) {
        return SingleResponse.of(UserAdapterConverter.toDto(userManage.create(request.name(), request.email())));
    }

    @GetMapping("/{userId}")
    public SingleResponse<UserDTO> getById(@PathVariable String userId) {
        return SingleResponse.of(UserAdapterConverter.toDto(userManage.getById(userId)));
    }
}
