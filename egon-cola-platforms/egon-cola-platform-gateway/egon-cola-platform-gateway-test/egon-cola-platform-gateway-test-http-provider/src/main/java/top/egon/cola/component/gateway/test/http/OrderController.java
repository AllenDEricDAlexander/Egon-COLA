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
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;

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
            idempotent = true,
            tags = {"query"}
    )
    public OrderView get(
            @PathVariable("id")
            @GatewaySchemaField(description = "订单编号") String id,
            @RequestHeader(value = "X-Request-Source",
                    defaultValue = "unknown")
            @GatewaySchemaField(description = "请求来源") String source) {
        return new OrderView(id, "CREATED", source);
    }

    @PostMapping
    @GatewayOperation(
            name = "创建订单",
            summary = "创建新的测试订单",
            owner = "gateway-test",
            externalAccessible = true,
            tags = {"command"}
    )
    public ResponseEntity<OrderView> create(
            @RequestBody
            @GatewaySchemaField(description = "创建订单命令")
            CreateOrder command) {
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
            idempotent = true,
            tags = {"query"}
    )
    public SearchResult search(
            @RequestParam("customerId")
            @GatewaySchemaField(description = "客户编号")
            String customerId,
            @RequestParam(value = "limit", defaultValue = "10")
            @GatewaySchemaField(description = "返回数量上限")
            int limit) {
        return new SearchResult(customerId, limit, Math.min(limit, 2));
    }

    @PostMapping("/{id}/cancel")
    @GatewayOperation(
            name = "取消订单",
            externalAccessible = true,
            idempotent = true,
            tags = {"command"}
    )
    public OrderView cancel(
            @PathVariable("id")
            @GatewaySchemaField(description = "订单编号") String id,
            @RequestHeader(value = "Idempotency-Key", defaultValue = "")
            @GatewaySchemaField(description = "幂等键")
            String idempotencyKey) {
        return new OrderView(id, "CANCELLED", idempotencyKey);
    }

    public record CreateOrder(
            @GatewaySchemaField(description = "客户编号") String customerId,
            @GatewaySchemaField(description = "下单渠道") String channel
    ) {
    }

    public record OrderView(
            @GatewaySchemaField(description = "订单编号") String id,
            @GatewaySchemaField(description = "订单状态") String status,
            @GatewaySchemaField(description = "请求来源或业务渠道")
            String source
    ) {
    }

    public record SearchResult(
            @GatewaySchemaField(description = "客户编号") String customerId,
            @GatewaySchemaField(description = "实际查询上限") int limit,
            @GatewaySchemaField(description = "匹配订单数量") int count
    ) {
    }
}
