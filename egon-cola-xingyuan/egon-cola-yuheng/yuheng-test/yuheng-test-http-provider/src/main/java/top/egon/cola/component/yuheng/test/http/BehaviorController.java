package top.egon.cola.component.yuheng.test.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

@RestController
@RequestMapping("/api")
@Tag(name = "orders")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台域",
        entityDomainCode = "gateway-test",
        entityDomainName = "网关测试实体域",
        interfaceGroupCode = "orders"
)
public class BehaviorController {

    @GetMapping("/slow/{millis}")
    @Operation(
            operationId = "orders.slow",
            summary = "延迟响应",
            tags = {"failure-test"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.FALSE
    )
    public DelayView slow(@PathVariable("millis") long millis)
            throws InterruptedException {
        long bounded = Math.max(0, Math.min(millis, 10_000));
        Thread.sleep(bounded);
        return new DelayView(bounded);
    }

    @GetMapping("/fail/{status}")
    @Operation(
            operationId = "orders.fail",
            summary = "指定状态失败",
            tags = {"failure-test"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.FALSE
    )
    public ResponseEntity<FailureView> fail(
            @PathVariable("status") int status) {
        int bounded = status >= 400 && status <= 599 ? status : 500;
        return ResponseEntity.status(bounded)
                .body(new FailureView("HTTP_PROVIDER_FAILURE", bounded));
    }

    @PostMapping("/body/echo")
    @Operation(
            operationId = "orders.bodyEcho",
            summary = "回显请求体",
            tags = {"body"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE
    )
    public byte[] echo(@RequestBody byte[] body) {
        return body;
    }

    public record DelayView(long elapsedMillis) {
    }

    public record FailureView(String code, int status) {
    }
}
