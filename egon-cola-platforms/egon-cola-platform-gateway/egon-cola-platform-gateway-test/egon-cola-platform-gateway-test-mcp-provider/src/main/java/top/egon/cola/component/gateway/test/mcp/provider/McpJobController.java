package top.egon.cola.component.gateway.test.mcp.provider;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;

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
@GatewayInterfaceGroup(
        businessDomainCode = "mcp-test",
        businessDomainName = "MCP 测试域",
        entityDomainCode = "fixture",
        entityDomainName = "Fixture",
        code = "mcp-fixture-operations",
        name = "MCP Fixture Operations",
        description = "完全本地且确定性的 MCP Operation 测试接口"
)
public class McpJobController {

    private final AtomicLong sequence = new AtomicLong();

    private final Map<String, JobView> jobs = new ConcurrentHashMap<>();

    @PostMapping("/echo")
    @GatewayOperation(
            name = "MCP Echo",
            summary = "返回原始本地输入",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"mcp", "query", "idempotent"}
    )
    public EchoView echo(@RequestBody EchoCommand command) {
        return new EchoView(command.value(), "HTTP");
    }

    @GetMapping("/query")
    @GatewayOperation(
            name = "MCP Query",
            summary = "确定性查询",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"mcp", "query", "idempotent"},
            responseSchemaFields = @GatewaySchemaField(
                    path = "items",
                    description = "排序后的确定性结果"
            )
    )
    public QueryView query(
            @RequestParam(value = "prefix", defaultValue = "fixture")
            String prefix) {
        return new QueryView(List.of(prefix + "-1", prefix + "-2"));
    }

    @PostMapping("/write")
    @ResponseStatus(HttpStatus.CREATED)
    @GatewayOperation(
            name = "MCP Write",
            summary = "确定性写操作",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"mcp", "command", "idempotent"}
    )
    public WriteView write(@RequestBody WriteCommand command) {
        return new WriteView("write-" + command.key(), command.value());
    }

    @PostMapping("/high-risk")
    @GatewayOperation(
            name = "MCP High Risk",
            summary = "用于一次性审批验证的高风险操作",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"mcp", "command", "high-risk"}
    )
    public ApprovalView highRisk(@RequestBody ApprovalCommand command) {
        return new ApprovalView(command.action(), "APPROVED_FIXTURE");
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @GatewayOperation(
            name = "MCP Start Job",
            summary = "创建可恢复或等待输入的本地任务",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"mcp", "job", "command"}
    )
    public JobView startJob(@RequestBody StartJob command) {
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
    @GatewayOperation(
            name = "MCP List Jobs",
            summary = "列出确定性本地任务",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"mcp", "job", "query"}
    )
    public List<JobView> jobs() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(JobView::id))
                .toList();
    }

    @GetMapping("/jobs/{id}")
    @GatewayOperation(
            name = "MCP Get Job",
            summary = "读取本地任务",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"mcp", "job", "query"}
    )
    public JobView job(@PathVariable String id) {
        return required(id);
    }

    @PostMapping("/jobs/{id}/input")
    @GatewayOperation(
            name = "MCP Submit Job Input",
            summary = "向等待输入的任务提交一次输入",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"mcp", "job", "command", "idempotent"}
    )
    public JobView submitInput(
            @PathVariable String id,
            @RequestBody JobInput input) {
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
    @GatewayOperation(
            name = "MCP Cancel Job",
            summary = "取消未结束的本地任务",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"mcp", "job", "command", "idempotent"}
    )
    public JobView cancelJob(@PathVariable String id) {
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

    public record EchoCommand(String value) {
    }

    public record EchoView(String value, String protocol) {
    }

    public record QueryView(List<String> items) {
    }

    public record WriteCommand(String key, String value) {
    }

    public record WriteView(String id, String value) {
    }

    public record ApprovalCommand(String action) {
    }

    public record ApprovalView(String action, String outcome) {
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
