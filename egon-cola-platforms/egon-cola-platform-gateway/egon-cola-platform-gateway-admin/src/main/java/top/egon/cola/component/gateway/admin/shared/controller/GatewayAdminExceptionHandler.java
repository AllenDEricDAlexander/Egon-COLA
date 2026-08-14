package top.egon.cola.component.gateway.admin.shared.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.component.gateway.admin.application.domain.exception.GatewayApplicationAlreadyExistsException;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.McpValidationException;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
import top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO;
import top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminFieldErrorVO;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;

import java.time.Instant;
import java.util.List;

/**
 * 中文说明：{@code GatewayAdminExceptionHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责网关管理端Exception处理器相关的职责与边界。
 * English summary: {@code GatewayAdminExceptionHandler} is a gateway admin exception handler handler in the current Gateway module; it owns the gateway admin exception handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestControllerAdvice
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-gateway-admin-exception-handler",
        name = "GatewayAdminExceptionHandler 管理接口组")
public class GatewayAdminExceptionHandler {

    /**
     * 中文说明：执行 制品Conflict 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the artifact conflict operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.artifactConflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 制品Conflict 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(McpAppArtifactStore.ArtifactConflictException.class)
    public ResponseEntity<GatewayAdminErrorVO> artifactConflict(
            McpAppArtifactStore.ArtifactConflictException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new GatewayAdminErrorVO(
                        "GATEWAY_MCP_ARTIFACT_IMMUTABLE",
                        error.getMessage(),
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 制品Rejected 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the artifact rejected operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.artifactRejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 制品Rejected 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(McpAppArtifactStore.ArtifactRejectedException.class)
    public ResponseEntity<GatewayAdminErrorVO> artifactRejected(
            McpAppArtifactStore.ArtifactRejectedException error) {
        return ResponseEntity.unprocessableEntity().body(
                new GatewayAdminErrorVO(
                        "GATEWAY_MCP_ARTIFACT_REJECTED",
                        error.getMessage(),
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 MCPValidation 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mcp validation operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.mcpValidation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 MCPValidation 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(McpValidationException.class)
    public ResponseEntity<GatewayAdminErrorVO> mcpValidation(
            McpValidationException error) {
        return ResponseEntity.badRequest().body(new GatewayAdminErrorVO(
                error.code(),
                error.getMessage(),
                null,
                List.of(new GatewayAdminFieldErrorVO(
                        error.path(),
                        error.code(),
                        error.getMessage()
                )),
                Instant.now()
        ));
    }

    /**
     * 中文说明：执行 applicationExists 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the application exists operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.applicationExists(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 applicationExists 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(GatewayApplicationAlreadyExistsException.class)
    public ResponseEntity<GatewayAdminErrorVO> applicationExists(
            GatewayApplicationAlreadyExistsException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new GatewayAdminErrorVO(
                        "GATEWAY_ADMIN_APPLICATION_ALREADY_EXISTS",
                        error.getMessage(),
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 revision 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revision operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.revision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 revision 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(GatewayAdminRevisionConflictException.class)
    public ResponseEntity<GatewayAdminErrorVO> revision(
            GatewayAdminRevisionConflictException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new GatewayAdminErrorVO(
                        "GATEWAY_ADMIN_REVISION_CONFLICT",
                        "draft or resource revision is stale",
                        error.currentRevision(),
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 idempotency 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the idempotency operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.idempotency(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 idempotency 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(GatewayAdminIdempotencyConflictException.class)
    public ResponseEntity<GatewayAdminErrorVO> idempotency(
            GatewayAdminIdempotencyConflictException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new GatewayAdminErrorVO(
                        "GATEWAY_ADMIN_IDEMPOTENCY_CONFLICT",
                        error.getMessage(),
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 optimisticLock 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optimistic lock operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.optimisticLock(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 optimisticLock 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<GatewayAdminErrorVO> optimisticLock(
            ObjectOptimisticLockingFailureException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new GatewayAdminErrorVO(
                        "GATEWAY_ADMIN_REVISION_CONFLICT",
                        "resource was modified concurrently",
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 uniqueConflict 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the unique conflict operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.uniqueConflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 uniqueConflict 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GatewayAdminErrorVO> uniqueConflict(
            DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new GatewayAdminErrorVO(
                        "GATEWAY_ADMIN_RESOURCE_CONFLICT",
                        "resource violates a uniqueness or reference constraint",
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 notFound 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the not found operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.notFound(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 notFound 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(GatewayAdminNotFoundException.class)
    public ResponseEntity<GatewayAdminErrorVO> notFound(
            GatewayAdminNotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new GatewayAdminErrorVO(
                        "GATEWAY_ADMIN_NOT_FOUND",
                        error.getMessage(),
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 validation 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validation operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.validation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 validation 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GatewayAdminErrorVO> validation(
            MethodArgumentNotValidException error) {
        List<GatewayAdminFieldErrorVO> fields = error.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(item -> new GatewayAdminFieldErrorVO(
                        item.getField(),
                        "INVALID",
                        item.getDefaultMessage()
                ))
                .toList();
        return ResponseEntity.unprocessableEntity().body(
                new GatewayAdminErrorVO(
                        "GATEWAY_ADMIN_VALIDATION_FAILED",
                        "request validation failed",
                        null,
                        fields,
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 invalid 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.invalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 invalid 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GatewayAdminErrorVO> invalid(
            IllegalArgumentException error) {
        return ResponseEntity.unprocessableEntity().body(
                new GatewayAdminErrorVO(
                        "GATEWAY_ADMIN_VALIDATION_FAILED",
                        error.getMessage(),
                        null,
                        List.of(new GatewayAdminFieldErrorVO(
                                "$",
                                "INVALID",
                                error.getMessage()
                        )),
                        Instant.now()
                )
        );
    }

    /**
     * 中文说明：执行 invalidState 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid state operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.invalidState(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param error 参数 error；parameter error。
     * @return 返回 invalidState 的处理结果；returns the result of the operation.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<GatewayAdminErrorVO> invalidState(
            IllegalStateException error) {
        boolean unavailable = error.getMessage() != null
                && (error.getMessage().contains("DDC")
                || error.getMessage().contains("PROTECTOR"));
        String code = unavailable
                ? "GATEWAY_ADMIN_DDC_UNAVAILABLE"
                : errorCode(error.getMessage());
        return ResponseEntity.status(
                unavailable
                        ? HttpStatus.SERVICE_UNAVAILABLE
                        : HttpStatus.CONFLICT
        ).body(new GatewayAdminErrorVO(
                code,
                error.getMessage(),
                null,
                List.of(),
                Instant.now()
        ));
    }

    /**
     * 中文说明：执行 errorCode 操作；该方法是 {@code GatewayAdminExceptionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the error code operation; this method is the invocation entry point on {@code GatewayAdminExceptionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminExceptionHandler.errorCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 errorCode 的处理结果；returns the result of the operation.
     */
    private String errorCode(String message) {
        if (message != null
                && message.startsWith("GATEWAY_ADMIN_")) {
            int separator = message.indexOf(':');
            return separator < 0
                    ? message
                    : message.substring(0, separator);
        }
        return "GATEWAY_ADMIN_RESOURCE_CONFLICT";
    }




}
