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
    public EchoView echo(
            @RequestParam("message") String message,
            @RequestHeader(value = "traceparent", required = false)
            String traceparent,
            @RequestHeader(value = "x-egon-request-id", required = false)
            String requestId) {
        return traced(
                traceparent,
                requestId,
                () -> EchoView.from(client.echo(message))
        );
    }

    @GetMapping("/orders")
    public OrderView order(
            @RequestParam("orderId") String orderId,
            @RequestHeader(value = "traceparent", required = false)
            String traceparent,
            @RequestHeader(value = "x-egon-request-id", required = false)
            String requestId) {
        return traced(
                traceparent,
                requestId,
                () -> OrderView.from(client.order(orderId))
        );
    }

    @PostMapping("/orders")
    public OrderView create(
            @RequestBody CreateOrder command,
            @RequestHeader(value = "traceparent", required = false)
            String traceparent,
            @RequestHeader(value = "x-egon-request-id", required = false)
            String requestId) {
        return traced(
                traceparent,
                requestId,
                () -> OrderView.from(client.create(
                        command.customerId(),
                        command.skus()
                ))
        );
    }

    @GetMapping("/slow")
    public OrderView slow(
            @RequestParam("orderId") String orderId,
            @RequestParam("delayMillis") long delayMillis,
            @RequestHeader(value = "traceparent", required = false)
            String traceparent,
            @RequestHeader(value = "x-egon-request-id", required = false)
            String requestId) {
        return traced(
                traceparent,
                requestId,
                () -> OrderView.from(client.slow(orderId, delayMillis))
        );
    }

    @GetMapping("/fail")
    public OrderView fail(
            @RequestParam("code") String code,
            @RequestHeader(value = "traceparent", required = false)
            String traceparent,
            @RequestHeader(value = "x-egon-request-id", required = false)
            String requestId) {
        return traced(
                traceparent,
                requestId,
                () -> OrderView.from(client.fail(code))
        );
    }

    private <T> T traced(String traceparent,
                         String requestId,
                         Supplier<T> invocation) {
        TraceContext context = TraceContext.fromHeaders(
                name -> {
                    if (TraceContext.TRACEPARENT_HEADER.equals(name)) {
                        return traceparent;
                    }
                    if (TraceContext.REQUEST_ID_HEADER.equals(name)) {
                        return requestId;
                    }
                    return null;
                },
                false
        );
        try (TraceContext.Scope ignored = context.open()) {
            return invocation.get();
        }
    }

    public record CreateOrder(String customerId, List<String> skus) {

        public CreateOrder {
            skus = skus == null ? List.of() : List.copyOf(skus);
        }
    }

    public record EchoView(
            String providerId,
            String message,
            String invocationId,
            String traceId
    ) {

        private static EchoView from(EchoResponse response) {
            return new EchoView(
                    response.getProviderId(),
                    response.getMessage(),
                    response.getInvocationId(),
                    response.getTraceId()
            );
        }
    }

    public record OrderView(
            String orderId,
            String status,
            String providerId
    ) {

        private static OrderView from(OrderResponse response) {
            return new OrderView(
                    response.getOrderId(),
                    response.getStatus(),
                    response.getProviderId()
            );
        }
    }
}
