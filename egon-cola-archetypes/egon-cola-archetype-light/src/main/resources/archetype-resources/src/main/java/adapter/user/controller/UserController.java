package ${package}.adapter.user.controller;

import ${package}.adapter.filter.RequestContext;
import ${package}.adapter.filter.RequestContextHolder;
import ${package}.adapter.user.convertor.UserAdapterConvertor;
import ${package}.adapter.user.dto.CreateUserRequest;
import ${package}.adapter.user.vo.UserDetailVO;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.manage.UserManage;
import ${package}.application.user.query.GetUserQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserManage userManage;
    private final UserAdapterConvertor convertor;

    @PostMapping
    public UserDetailVO create(@Valid @RequestBody CreateUserRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toUserDetail(userManage.create(new CreateUserCommand(
                request.externalId(),
                request.name(),
                request.email(),
                context.operatorId(),
                context.requestId())));
    }

    @GetMapping("/{userId}")
    public UserDetailVO get(@PathVariable String userId) {
        return convertor.toUserDetail(userManage.get(new GetUserQuery(userId)));
    }
}
