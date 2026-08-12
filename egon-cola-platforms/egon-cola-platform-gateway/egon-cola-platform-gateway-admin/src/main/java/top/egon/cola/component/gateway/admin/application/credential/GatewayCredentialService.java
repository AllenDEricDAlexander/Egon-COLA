package top.egon.cola.component.gateway.admin.application.credential;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayCredentialService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关凭证服务相关的职责与边界。
 * English summary: {@code GatewayCredentialService} is a gateway credential service service in the current Gateway module; it owns the gateway credential service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class GatewayCredentialService {

    /**
     * 中文说明：保存 applications 对应的状态、依赖或配置值；字段类型为 {@code GatewayApplicationRepository}，由 {@code GatewayCredentialService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by applications; its type is {@code GatewayApplicationRepository}, and {@code GatewayCredentialService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCredentialService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayApplicationRepository applications;

    /**
     * 中文说明：保存 credentials 对应的状态、依赖或配置值；字段类型为 {@code GatewayCredentialStore}，由 {@code GatewayCredentialService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by credentials; its type is {@code GatewayCredentialStore}, and {@code GatewayCredentialService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCredentialService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCredentialStore credentials;

    /**
     * 中文说明：保存 audits 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuditLogRepository}，由 {@code GatewayCredentialService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audits; its type is {@code GatewayAuditLogRepository}, and {@code GatewayCredentialService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCredentialService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAuditLogRepository audits;

    /**
     * 中文说明：保存 protector 对应的状态、依赖或配置值；字段类型为 {@code GatewaySecretProtector}，由 {@code GatewayCredentialService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by protector; its type is {@code GatewaySecretProtector}, and {@code GatewayCredentialService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCredentialService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewaySecretProtector protector;

    /**
     * 中文说明：保存 random 对应的状态、依赖或配置值；字段类型为 {@code SecureRandom}，由 {@code GatewayCredentialService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by random; its type is {@code SecureRandom}, and {@code GatewayCredentialService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCredentialService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final SecureRandom random;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayCredentialService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayCredentialService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCredentialService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayCredentialService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCredentialService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param applications 参数 applications；parameter applications。
     * @param credentials 参数 credentials；parameter credentials。
     * @param audits 参数 audits；parameter audits。
     * @param protector 参数 protector；parameter protector。
     */
    @Autowired
    public GatewayCredentialService(
            GatewayApplicationRepository applications,
            GatewayCredentialStore credentials,
            GatewayAuditLogRepository audits,
            ObjectProvider<GatewaySecretProtector> protector) {
        this(
                applications,
                credentials,
                audits,
                protector.getIfAvailable(),
                new SecureRandom(),
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code GatewayCredentialService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCredentialService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param applications 参数 applications；parameter applications。
     * @param credentials 参数 credentials；parameter credentials。
     * @param audits 参数 audits；parameter audits。
     * @param protector 参数 protector；parameter protector。
     * @param random 参数 random；parameter random。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayCredentialService(
            GatewayApplicationRepository applications,
            GatewayCredentialStore credentials,
            GatewayAuditLogRepository audits,
            GatewaySecretProtector protector,
            SecureRandom random,
            Clock clock) {
        this.applications = applications;
        this.credentials = credentials;
        this.audits = audits;
        this.protector = protector;
        this.random = random;
        this.clock = clock;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<CredentialView> list(String applicationId) {
        requireApplication(applicationId);
        return credentials.list(applicationId).stream()
                .map(credential -> new CredentialView(
                        credential.id(),
                        credential.accessKey(),
                        credential.status(),
                        credential.validFrom(),
                        credential.validUntil()
                ))
                .toList();
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    @Transactional
    public IssuedCredential create(
            String applicationId,
            AdminActor actor,
            RequestAuditContext request) {
        requireApplication(applicationId);
        GatewaySecretProtector configured = requireProtector();
        String id = UuidV7.simpleString();
        String accessKey = "gw_" + token(18);
        String secret = token(32);
        Instant now = clock.instant();
        GatewaySecretProtector.ProtectedSecret protectedSecret =
                configured.protect(secret, aad(applicationId, accessKey));
        credentials.insert(new GatewayCredentialStore.CredentialRecord(
                id,
                applicationId,
                accessKey,
                protectedSecret.ciphertext(),
                protectedSecret.keyVersion(),
                "ACTIVE",
                now,
                null,
                now,
                now
        ));
        audit(actor, request, applicationId, accessKey, "CREATE");
        return new IssuedCredential(
                id,
                accessKey,
                secret,
                "ACTIVE",
                now,
                null
        );
    }

    /**
     * 中文说明：执行 rotate 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rotate operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.rotate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param keyId 参数 键Id；parameter key id。
     * @param overlap 参数 overlap；parameter overlap。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 rotate 的处理结果；returns the result of the operation.
     */
    @Transactional
    public IssuedCredential rotate(
            String applicationId,
            String keyId,
            Duration overlap,
            AdminActor actor,
            RequestAuditContext request) {
        if (overlap.isNegative() || overlap.compareTo(
                Duration.ofHours(24)
        ) > 0) {
            throw new IllegalArgumentException(
                    "credential overlap must be between 0 and 24 hours"
            );
        }
        GatewayCredentialStore.CredentialRecord current = required(
                applicationId,
                keyId
        );
        if ("REVOKED".equals(current.status())) {
            throw new IllegalArgumentException(
                    "revoked credential cannot be rotated"
            );
        }
        Instant now = clock.instant();
        credentials.overlap(
                current.id(),
                now.plus(overlap),
                now
        );
        IssuedCredential replacement = create(
                applicationId,
                actor,
                request
        );
        audit(actor, request, applicationId, current.accessKey(), "ROTATE");
        return replacement;
    }

    /**
     * 中文说明：执行 revoke 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.revoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param keyId 参数 键Id；parameter key id。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 revoke 的处理结果；returns the result of the operation.
     */
    @Transactional
    public CredentialView revoke(
            String applicationId,
            String keyId,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayCredentialStore.CredentialRecord credential = required(
                applicationId,
                keyId
        );
        Instant now = clock.instant();
        credentials.revoke(credential.id(), now);
        audit(actor, request, applicationId, credential.accessKey(), "REVOKE");
        return new CredentialView(
                credential.id(),
                credential.accessKey(),
                "REVOKED",
                credential.validFrom(),
                now
        );
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param keyId 参数 键Id；parameter key id。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private GatewayCredentialStore.CredentialRecord required(
            String applicationId,
            String keyId) {
        return credentials.find(applicationId, keyId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway credential " + keyId + " was not found"
                ));
    }

    /**
     * 中文说明：执行 requireApplication 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require application operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.requireApplication(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     */
    private void requireApplication(String applicationId) {
        if (applications.findByIdAndDeletedFalse(applicationId).isEmpty()) {
            throw new GatewayAdminNotFoundException(
                    "gateway application "
                            + applicationId
                            + " was not found"
            );
        }
    }

    /**
     * 中文说明：执行 requireProtector 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require protector operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.requireProtector(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 requireProtector 的处理结果；returns the result of the operation.
     */
    private GatewaySecretProtector requireProtector() {
        if (protector == null) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_SECRET_PROTECTOR_NOT_CONFIGURED"
            );
        }
        return protector;
    }

    /**
     * 中文说明：执行 token 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the token operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.token(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bytes 参数 bytes；parameter bytes。
     * @return 返回 token 的处理结果；returns the result of the operation.
     */
    private String token(int bytes) {
        byte[] value = new byte[bytes];
        random.nextBytes(value);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    /**
     * 中文说明：执行 aad 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the aad operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.aad(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param accessKey 参数 access键；parameter access key。
     * @return 返回 aad 的处理结果；returns the result of the operation.
     */
    private String aad(String applicationId, String accessKey) {
        return applicationId + ":" + accessKey;
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayCredentialService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayCredentialService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialService.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @param applicationId 参数 applicationId；parameter application id。
     * @param accessKey 参数 access键；parameter access key。
     * @param action 参数 action；parameter action。
     */
    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String applicationId,
            String accessKey,
            String action) {
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                "GATEWAY_CREDENTIAL",
                accessKey,
                action,
                null,
                Map.of("applicationId", applicationId),
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }

    /**
     * 中文说明：{@code IssuedCredential} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Issued凭证相关的职责与边界。
     * English summary: {@code IssuedCredential} is an immutable data carrier in the current Gateway module; it owns the issued credential-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param accessKey 参数 access键；parameter access key。
     * @param secret 参数 secret；parameter secret。
     * @param status 参数 status；parameter status。
     * @param validFrom 参数 validFrom；parameter valid from。
     * @param validUntil 参数 validUntil；parameter valid until。
     */
    public record IssuedCredential(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialService.IssuedCredential} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCredentialService.IssuedCredential} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.IssuedCredential} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.IssuedCredential}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 access键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialService.IssuedCredential} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by access key; its type is {@code String}, and {@code GatewayCredentialService.IssuedCredential} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.IssuedCredential} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.IssuedCredential}; do not couple callers to its representation when the owning type exposes an API.
             */
            String accessKey,
            /**
             * 中文说明：保存 secret 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialService.IssuedCredential} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by secret; its type is {@code String}, and {@code GatewayCredentialService.IssuedCredential} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.IssuedCredential} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.IssuedCredential}; do not couple callers to its representation when the owning type exposes an API.
             */
            String secret,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialService.IssuedCredential} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code GatewayCredentialService.IssuedCredential} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.IssuedCredential} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.IssuedCredential}; do not couple callers to its representation when the owning type exposes an API.
             */
            String status,
            /**
             * 中文说明：保存 validFrom 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCredentialService.IssuedCredential} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by valid from; its type is {@code Instant}, and {@code GatewayCredentialService.IssuedCredential} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.IssuedCredential} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.IssuedCredential}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant validFrom,
            /**
             * 中文说明：保存 validUntil 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCredentialService.IssuedCredential} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by valid until; its type is {@code Instant}, and {@code GatewayCredentialService.IssuedCredential} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.IssuedCredential} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.IssuedCredential}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant validUntil
    ) {
    }

    /**
     * 中文说明：{@code CredentialView} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责凭证View相关的职责与边界。
     * English summary: {@code CredentialView} is an immutable data carrier in the current Gateway module; it owns the credential view-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param accessKey 参数 access键；parameter access key。
     * @param status 参数 status；parameter status。
     * @param validFrom 参数 validFrom；parameter valid from。
     * @param validUntil 参数 validUntil；parameter valid until。
     */
    public record CredentialView(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialService.CredentialView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCredentialService.CredentialView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.CredentialView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.CredentialView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 access键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialService.CredentialView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by access key; its type is {@code String}, and {@code GatewayCredentialService.CredentialView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.CredentialView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.CredentialView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String accessKey,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialService.CredentialView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code GatewayCredentialService.CredentialView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.CredentialView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.CredentialView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String status,
            /**
             * 中文说明：保存 validFrom 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCredentialService.CredentialView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by valid from; its type is {@code Instant}, and {@code GatewayCredentialService.CredentialView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.CredentialView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.CredentialView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant validFrom,
            /**
             * 中文说明：保存 validUntil 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCredentialService.CredentialView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by valid until; its type is {@code Instant}, and {@code GatewayCredentialService.CredentialView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialService.CredentialView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialService.CredentialView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant validUntil
    ) {
    }
}
