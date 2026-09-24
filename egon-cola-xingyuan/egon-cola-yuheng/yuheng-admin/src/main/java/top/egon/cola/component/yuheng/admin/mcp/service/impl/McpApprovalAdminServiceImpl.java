package top.egon.cola.component.yuheng.admin.mcp.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpApprovalBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpApprovalRequestDTO;
import top.egon.cola.component.yuheng.admin.mcp.domain.vo.McpApprovalOwnerVO;
import top.egon.cola.component.yuheng.admin.mcp.domain.vo.McpApprovalVO;
import top.egon.cola.component.yuheng.admin.mcp.repository.McpApprovalRepository;
import top.egon.cola.component.yuheng.admin.mcp.service.McpApprovalAdminService;
import top.egon.cola.component.yuheng.mcp.common.security.McpSecurityDigests;

/**
 * 中文说明：{@code McpApprovalAdminServiceImpl} 是 {@link McpApprovalAdminService} 的实现，逐字沿用原控制器编排：
 * 生成 32 字节 URL-safe 明文令牌与随机审批标识，只把令牌与参数的摘要交给受守卫的 {@link McpApprovalRepository} 端口，
 * 并在同一写事务内落库；业务字段、状态与错误语义与原 HTTP 合同一致。
 * English summary: {@code McpApprovalAdminServiceImpl} implements {@link McpApprovalAdminService} and keeps the original
 * controller orchestration verbatim: a 32-byte URL-safe plaintext token plus a random approval identifier, only the token
 * and argument digests handed to the guarded {@link McpApprovalRepository} port inside one write transaction; business
 * fields, states and error semantics match the original HTTP contract.
 *
 * 用法 / Usage: 由 {@code McpApprovalController} 注入调用；/ Injected and invoked by the approval controller.
 */
@Slf4j
@Validated
@Service("mcpApprovalAdminServiceImpl")
public class McpApprovalAdminServiceImpl implements McpApprovalAdminService {

    /**
     * 中文说明：表示 TOKENBYTES 这一固定值；它属于 {@code McpApprovalAdminServiceImpl} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value token bytes; it is a state, type, or protocol value of
     * {@code McpApprovalAdminServiceImpl} and keeps callers aligned with the owning type.
     */
    private static final int TOKEN_BYTES = 32;

    private final McpApprovalRepository approvals;

    private final ObjectMapper objectMapper;

    private final SecureRandom random;

    private final Clock clock;

    /**
     * 中文说明：创建 {@code McpApprovalAdminServiceImpl} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpApprovalAdminServiceImpl} from the dependencies or initial data
     * required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器调用；/ Call it from the Spring container after validating the supplied dependencies.
     * @param approvals 参数 受守卫审批端口；parameter the guarded approval port。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    @Autowired
    public McpApprovalAdminServiceImpl(
            @Qualifier("mpMcpApprovalRepository") McpApprovalRepository approvals,
            ObjectMapper objectMapper) {
        this(
                approvals,
                objectMapper,
                new SecureRandom(),
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code McpApprovalAdminServiceImpl} 实例，并接收构建该实例所需的依赖或初始数据；受控随机源与时钟用于合同的确定性验证。
     * English summary: Creates an instance of {@code McpApprovalAdminServiceImpl} from the dependencies or initial data
     * required at construction time; the supplied randomness and clock allow deterministic contract verification.
     *
     * 用法 / Usage: 由所属类型的受控入口调用；/ Call it from the owning type's controlled entry points.
     * @param approvals 参数 受守卫审批端口；parameter the guarded approval port。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param random 参数 受控随机源；parameter the controlled randomness source。
     * @param clock 参数 受控时钟；parameter the controlled clock。
     */
    public McpApprovalAdminServiceImpl(
            McpApprovalRepository approvals,
            ObjectMapper objectMapper,
            SecureRandom random,
            Clock clock) {
        this.approvals = Objects.requireNonNull(approvals, "approvals");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.random = Objects.requireNonNull(random, "random");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 中文说明：执行 issue 操作；该方法是 {@code McpApprovalAdminServiceImpl} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the issue operation; this method is the invocation entry point on
     * {@code McpApprovalAdminServiceImpl} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpApprovalAdminServiceImpl.issue(request, owner)}。
     * @param request 参数 审批请求；parameter the approval request。
     * @param owner 参数 已验证的受信归属；parameter the verified trusted owner。
     * @return 返回 issue 的处理结果；returns the result of the operation.
     */
    @Override
    @Transactional
    public McpApprovalVO issue(
            McpApprovalRequestDTO request,
            McpApprovalOwnerVO owner) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(request.ttlSeconds());
        String token = token();
        String id = UUID.randomUUID().toString();
        approvals.issue(McpApprovalBO.normalized(
                id,
                McpSecurityDigests.token(token),
                owner.subjectId(),
                owner.tenantId(),
                owner.clientId(),
                request.serverCode(),
                request.toolName(),
                McpSecurityDigests.arguments(
                        objectMapper,
                        request.arguments()
                ),
                issuedAt,
                expiresAt
        ));
        log.info(
                "YUHENG_MCP_APPROVAL_ISSUED approvalId={} serverCode={} toolName={} expiresAt={}",
                id,
                request.serverCode(),
                request.toolName(),
                expiresAt
        );
        return new McpApprovalVO(id, token, expiresAt);
    }

    /**
     * 中文说明：执行 token 操作；该方法是 {@code McpApprovalAdminServiceImpl} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the token operation; this method is the invocation entry point on
     * {@code McpApprovalAdminServiceImpl} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 由所属类型的受控入口调用；/ Call it from the owning type's controlled entry points.
     * @return 返回 token 的处理结果；returns the result of the operation.
     */
    private String token() {
        byte[] value = new byte[TOKEN_BYTES];
        random.nextBytes(value);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }
}
