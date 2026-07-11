package ${package}.adapter.controller.user;

import ${package}.adapter.converter.UserAdapterConverter;
import ${package}.adapter.dto.user.CreateUserRequest;
import ${package}.adapter.vo.user.UserDetailVO;
import ${package}.application.manage.user.UserManage;
import ${package}.application.query.user.UserDetailQuery;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController("userController")
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManage userManage;
    private final UserAdapterConverter converter;

    public UserController(UserManage userManage, UserAdapterConverter converter) {
        this.userManage = userManage;
        this.converter = converter;
    }

    @PostMapping
    public ResponseEntity<UserDetailVO> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateUserRequest request) {
        String requestId = idempotencyKey == null ? UUID.randomUUID().toString() : idempotencyKey;
        UserDetailVO body = converter.toVO(userManage.createUser(converter.toCommand(requestId, request)));
        return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
    }

    @GetMapping("/{userId}")
    public UserDetailVO get(@PathVariable String userId) {
        return converter.toVO(userManage.getUser(new UserDetailQuery(userId)));
    }
}
