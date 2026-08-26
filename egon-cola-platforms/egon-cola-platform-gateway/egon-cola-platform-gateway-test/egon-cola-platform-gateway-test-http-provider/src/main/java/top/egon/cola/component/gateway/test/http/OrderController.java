package top.egon.cola.component.gateway.test.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "orders", description = "订单接口组")
@EgonApiCatalog(
        businessDomainCode = "commerce",
        businessDomainName = "交易域",
        entityDomainCode = "order",
        entityDomainName = "订单实体域",
        interfaceGroupCode = "orders"
)
public class OrderController {

    @GetMapping("/{id}")
    @Operation(
            operationId = "orders.get",
            summary = "按订单 ID 查询",
            tags = {"query"}
    )
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE
    )
    public OrderView get(
            @PathVariable("id")
            @Parameter(description = "订单编号") String id,
            @RequestHeader(value = "X-Request-Source",
                    defaultValue = "unknown")
            @Parameter(description = "请求来源") String source) {
        return new OrderView(id, "CREATED", source);
    }

    @PostMapping
    @Operation(
            operationId = "orders.create",
            summary = "创建新的测试订单",
            tags = {"command"}
    )
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.FALSE
    )
    public ResponseEntity<OrderView> create(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "创建订单命令")
            CreateOrder command) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderView(
                        "order-" + command.customerId(),
                        "CREATED",
                        command.channel()
                ));
    }

    @GetMapping("/search")
    @Operation(
            operationId = "orders.search",
            summary = "搜索订单",
            tags = {"query"}
    )
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE
    )
    public SearchResult search(
            @RequestParam("customerId")
            @Parameter(description = "客户编号")
            String customerId,
            @RequestParam(value = "limit", defaultValue = "10")
            @Parameter(description = "返回数量上限")
            int limit) {
        return new SearchResult(customerId, limit, Math.min(limit, 2));
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            operationId = "orders.cancel",
            summary = "取消订单",
            tags = {"command"}
    )
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE
    )
    public OrderView cancel(
            @PathVariable("id")
            @Parameter(description = "订单编号") String id,
            @RequestHeader(value = "Idempotency-Key", defaultValue = "")
            @Parameter(description = "幂等键")
            String idempotencyKey) {
        return new OrderView(id, "CANCELLED", idempotencyKey);
    }

    public record CreateOrder(
            @Schema(description = "客户编号") String customerId,
            @Schema(description = "下单渠道") String channel
    ) {
    }

    public record OrderView(
            @Schema(description = "订单编号") String id,
            @Schema(description = "订单状态") String status,
            @Schema(description = "请求来源或业务渠道")
            String source
    ) {
    }

    public record SearchResult(
            @Schema(description = "客户编号") String customerId,
            @Schema(description = "实际查询上限") int limit,
            @Schema(description = "匹配订单数量") int count
    ) {
    }
}
