package top.egon.cola.component.rpc.test.mockgateway;

record MockGatewayInvocation(
        String invocationId,
        String fullMethodName,
        String providerInstanceId,
        String providerLeaseId
) {
}
