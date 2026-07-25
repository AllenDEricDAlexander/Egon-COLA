package top.egon.cola.component.gateway.test.rpc.consumer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoResponse;
import top.egon.cola.component.gateway.test.rpc.contract.proto.OrderResponse;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/test/rpc")
public class GatewayRpcDriverController {

    private final GatewayRpcTestClient client;

    public GatewayRpcDriverController(GatewayRpcTestClient client) {
        this.client = client;
    }

    @GetMapping("/echo")
    public EchoResponse echo(
            @RequestParam String message,
            @RequestHeader(value = "X-Trace-Id", required = false)
            String traceId) {
        return traced(traceId, () -> client.echo(message));
    }

    @GetMapping("/orders")
    public OrderResponse order(
            @RequestParam String orderId,
            @RequestHeader(value = "X-Trace-Id", required = false)
            String traceId) {
        return traced(traceId, () -> client.order(orderId));
    }

    @PostMapping("/orders")
    public OrderResponse create(
            @RequestBody CreateOrder command,
            @RequestHeader(value = "X-Trace-Id", required = false)
            String traceId) {
        return traced(
                traceId,
                () -> client.create(command.customerId(), command.skus())
        );
    }

    @GetMapping("/slow")
    public OrderResponse slow(
            @RequestParam String orderId,
            @RequestParam long delayMillis,
            @RequestHeader(value = "X-Trace-Id", required = false)
            String traceId) {
        return traced(
                traceId,
                () -> client.slow(orderId, delayMillis)
        );
    }

    @GetMapping("/fail")
    public OrderResponse fail(
            @RequestParam String code,
            @RequestHeader(value = "X-Trace-Id", required = false)
            String traceId) {
        return traced(traceId, () -> client.fail(code));
    }

    private <T> T traced(String traceId, Supplier<T> invocation) {
        String previous = TraceContext.getTraceId();
        try {
            TraceContext.setTraceId(traceId);
            return invocation.get();
        } finally {
            TraceContext.setTraceId(previous);
        }
    }

    public record CreateOrder(String customerId, List<String> skus) {

        public CreateOrder {
            skus = skus == null ? List.of() : List.copyOf(skus);
        }
    }
}
