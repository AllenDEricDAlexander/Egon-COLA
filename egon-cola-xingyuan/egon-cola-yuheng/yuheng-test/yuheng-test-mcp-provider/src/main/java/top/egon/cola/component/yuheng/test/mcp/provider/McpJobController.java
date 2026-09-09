package top.egon.cola.component.yuheng.test.mcp.provider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.contract.mcp.rule.McpRiskLevel;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.component.yuheng.openapi.annotation.EgonMcpTool;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic local Operations used by Tool, approval and durable Task tests.
 */
@RestController
@RequestMapping("/api/mcp-fixtures")
@Tag(name = "jobs", description = "MCP Fixture Operations")
@EgonApiCatalog(
        businessDomainCode = "mcp-test",
        businessDomainName = "MCP 测试域",
        entityDomainCode = "fixture",
        entityDomainName = "Fixture",
        interfaceGroupCode = "jobs"
)
public class McpJobController {

    private final AtomicLong sequence = new AtomicLong();

    private final Map<String, JobView> jobs = new ConcurrentHashMap<>();

    @PostMapping("/echo")
    @Operation(
            operationId = "jobs.echo",
            summary = "返回原始本地输入",
            tags = {"mcp", "query"})
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE)
    @EgonMcpTool(
            enabled = true,
            serverCode = "unified-local",
            name = "local_echo_task",
            permissions = {"mock:read"},
            riskLevel = McpRiskLevel.MEDIUM)
    public EchoView echo(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "回显命令")
            EchoCommand command) {
        return new EchoView(command.value(), "HTTP");
    }

    @GetMapping("/query")
    @Operation(
            operationId = "jobs.query",
            summary = "确定性查询",
            tags = {"mcp", "query"})
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE)
    @EgonMcpTool(
            enabled = true,
            serverCode = "unified-local",
            name = "local_query",
            permissions = {"mock:read"},
            riskLevel = McpRiskLevel.LOW)
    public QueryView query(
            @RequestParam(value = "prefix", defaultValue = "fixture")
            @Parameter(description = "查询前缀")
            String prefix) {
        return new QueryView(List.of(prefix + "-1", prefix + "-2"));
    }

    @PostMapping("/write")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "jobs.write",
            summary = "确定性写操作",
            tags = {"mcp", "command"})
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE)
    public WriteView write(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "写入命令")
            WriteCommand command) {
        return new WriteView("write-" + command.key(), command.value());
    }

    @PostMapping("/high-risk")
    @Operation(
            operationId = "jobs.highRisk",
            summary = "用于一次性审批验证的高风险操作",
            tags = {"mcp", "command", "high-risk"})
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.FALSE)
    @EgonMcpTool(
            enabled = true,
            serverCode = "unified-local",
            name = "high_risk_action",
            permissions = {"mock:admin"},
            riskLevel = McpRiskLevel.HIGH)
    public ApprovalView highRisk(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "审批命令")
            ApprovalCommand command) {
        return new ApprovalView(command.action(), "APPROVED_FIXTURE");
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            operationId = "jobs.startJob",
            summary = "创建可恢复或等待输入的本地任务",
            tags = {"mcp", "job", "command"})
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.FALSE)
    public JobView startJob(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "启动任务命令")
            StartJob command) {
        String id = "job-" + sequence.incrementAndGet();
        JobView created = new JobView(
                id,
                command.inputRequired() ? "INPUT_REQUIRED" : "WORKING",
                command.payload(),
                null,
                Instant.now()
        );
        jobs.put(id, created);
        return created;
    }

    @GetMapping("/jobs")
    @Operation(
            operationId = "jobs.list",
            summary = "列出确定性本地任务",
            tags = {"mcp", "job", "query"})
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.AUTO)
    public List<JobView> jobs() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(JobView::id))
                .toList();
    }

    @GetMapping("/jobs/{id}")
    @Operation(
            operationId = "jobs.get",
            summary = "读取本地任务",
            tags = {"mcp", "job", "query"})
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.AUTO)
    public JobView job(@PathVariable
                       @Parameter(description = "任务标识") String id) {
        return required(id);
    }

    @PostMapping("/jobs/{id}/input")
    @Operation(
            operationId = "jobs.submitInput",
            summary = "向等待输入的任务提交一次输入",
            tags = {"mcp", "job", "command"})
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE)
    public JobView submitInput(
            @PathVariable
            @Parameter(description = "任务标识") String id,
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "任务输入")
            JobInput input) {
        JobView current = required(id);
        if (!"INPUT_REQUIRED".equals(current.state())) {
            throw new IllegalStateException("job does not require input");
        }
        JobView completed = new JobView(
                id,
                "COMPLETED",
                current.payload(),
                input.value(),
                current.createdAt()
        );
        jobs.put(id, completed);
        return completed;
    }

    @PostMapping("/jobs/{id}/cancel")
    @Operation(
            operationId = "jobs.cancel",
            summary = "取消未结束的本地任务",
            tags = {"mcp", "job", "command"})
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE)
    public JobView cancelJob(@PathVariable
                             @Parameter(description = "任务标识") String id) {
        JobView current = required(id);
        JobView cancelled = new JobView(
                id,
                "CANCELLED",
                current.payload(),
                current.result(),
                current.createdAt()
        );
        jobs.put(id, cancelled);
        return cancelled;
    }

    private JobView required(String id) {
        JobView job = jobs.get(id);
        if (job == null) {
            throw new IllegalArgumentException("unknown fixture job: " + id);
        }
        return job;
    }

    public record EchoCommand(
            @Schema(description = "回显内容") String value
    ) {
    }

    public record EchoView(
            @Schema(description = "回显内容") String value,
            @Schema(description = "调用协议") String protocol
    ) {
    }

    public record QueryView(
            @Schema(description = "排序后的确定性结果")
            List<String> items
    ) {
    }

    public record WriteCommand(String key, String value) {
    }

    public record WriteView(String id, String value) {
    }

    public record ApprovalCommand(
            @Schema(description = "待审批动作") String action
    ) {
    }

    public record ApprovalView(
            @Schema(description = "审批动作") String action,
            @Schema(description = "审批结果") String outcome
    ) {
    }

    public record StartJob(String payload, boolean inputRequired) {
    }

    public record JobInput(String value) {
    }

    public record JobView(
            String id,
            String state,
            String payload,
            String result,
            Instant createdAt) {
    }
}
