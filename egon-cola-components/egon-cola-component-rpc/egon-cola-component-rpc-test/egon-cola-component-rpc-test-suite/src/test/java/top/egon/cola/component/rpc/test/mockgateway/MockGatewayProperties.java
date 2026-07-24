package top.egon.cola.component.rpc.test.mockgateway;

record MockGatewayProperties(
        String serviceName,
        String group,
        String version,
        String bindHost,
        int bindPort,
        String advertisedHost,
        int leaseSeconds,
        int heartbeatIntervalSeconds
) {

    static MockGatewayProperties defaults() {
        return new MockGatewayProperties(
                "egon-internal-rpc-gateway",
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
