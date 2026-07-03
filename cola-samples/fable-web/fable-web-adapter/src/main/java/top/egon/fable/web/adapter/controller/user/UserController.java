package top.egon.fable.web.adapter.controller.user;

import top.egon.fable.web.adapter.convertor.UserAdapterConverter;
import top.egon.fable.web.application.manage.user.UserManage;
import top.egon.fable.web.common.response.SingleResponse;
import top.egon.fable.web.facade.dto.PageResponse;
import top.egon.fable.web.facade.dto.user.CreateUserRequest;
import top.egon.fable.web.facade.dto.user.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("userController")
@RequestMapping("/users")
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
