package top.egon.cola.archetype.source.webopen.adapter.user.controller;

import top.egon.cola.archetype.source.webopen.adapter.user.pojo.convertor.UserAdapterConverter;
import top.egon.cola.archetype.source.webopen.adapter.user.pojo.dto.CreateUserRequest;
import top.egon.cola.archetype.source.webopen.adapter.user.pojo.vo.UserDetailVO;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.webopen.application.user.pojo.query.UserDetailQuery;
import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationFacadeSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.beans.factory.annotation.Qualifier;

@RestController("userController")
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    @Qualifier("userManage")
    private final UserManage userManage;
    @Qualifier("userAdapterConverterImpl")
    private final UserAdapterConverter converter;

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
        return converter.toVO(userManage.getUser(new UserDetailQuery(
                OrganizationFacadeSupport.positiveId(userId, "userId"))));
    }
}
