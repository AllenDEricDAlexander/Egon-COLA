package top.egon.cola.component.rpc.test.mockgateway;

public record MockGatewayProperties(
        String serviceName,
        String group,
        String version,
        String bindHost,
        int bindPort,
        String advertisedHost,
        int leaseSeconds,
        int heartbeatIntervalSeconds
) {

    public static MockGatewayProperties defaults() {
        return new MockGatewayProperties(
                "egon-gateway-rpc",
                "default",
                "1.0.0",
                "127.0.0.1",
                0,
                "127.0.0.1",
                20,
                5
        );
    }
}
