package top.egon.cola.component.gateway.test.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@GatewayInterfaceGroup(
        businessDomainCode = "commerce",
        businessDomainName = "交易域",
        entityDomainCode = "order",
        entityDomainName = "订单实体域",
        code = "order-query-command",
        name = "订单接口组",
        description = "真实 HTTP Provider 的订单接口"
)
public class OrderController {

    @GetMapping("/{id}")
    @GatewayOperation(
            name = "查询订单",
            summary = "按订单 ID 查询",
            owner = "gateway-test",
            externalAccessible = true,
            tags = {"query", "idempotent"}
    )
    public OrderView get(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Request-Source",
                    defaultValue = "unknown") String source) {
        return new OrderView(id, "CREATED", source);
    }

    @PostMapping
    @GatewayOperation(
            name = "创建订单",
            summary = "创建新的测试订单",
            owner = "gateway-test",
            externalAccessible = true,
            tags = {"command", "non-idempotent"}
    )
    public ResponseEntity<OrderView> create(
            @RequestBody CreateOrder command) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderView(
                        "order-" + command.customerId(),
                        "CREATED",
                        command.channel()
                ));
    }

    @GetMapping("/search")
    @GatewayOperation(
            name = "搜索订单",
            externalAccessible = true,
            tags = {"query", "idempotent"}
    )
    public Map<String, Object> search(
            @RequestParam("customerId") String customerId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Map.of(
                "customerId", customerId,
                "limit", limit,
                "count", Math.min(limit, 2)
        );
    }

    @PostMapping("/{id}/cancel")
    @GatewayOperation(
            name = "取消订单",
            externalAccessible = true,
            tags = {"command", "idempotent"}
    )
    public OrderView cancel(
            @PathVariable("id") String id,
            @RequestHeader(value = "Idempotency-Key",
                    defaultValue = "") String idempotencyKey) {
        return new OrderView(id, "CANCELLED", idempotencyKey);
    }

    public record CreateOrder(String customerId, String channel) {
    }

    public record OrderView(String id, String status, String source) {
    }
}
