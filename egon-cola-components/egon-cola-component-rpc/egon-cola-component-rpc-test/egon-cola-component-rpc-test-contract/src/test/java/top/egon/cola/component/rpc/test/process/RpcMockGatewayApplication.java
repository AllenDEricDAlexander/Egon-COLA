package top.egon.cola.component.rpc.test.process;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;
import top.egon.cola.component.ddc.client.registry.HttpDdcServiceRegistryClient;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.rpc.test.mockgateway.MockGatewayProperties;
import top.egon.cola.component.rpc.test.mockgateway.MockRpcGateway;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RpcMockGatewayApplication {

    private RpcMockGatewayApplication() {
    }

    public static void main(String[] arguments) throws Exception {
        Map<String, String> values = parse(arguments);
        DdcProperties properties = properties(values);
        RedissonClient redisson = redisson(properties);
        HttpDdcServiceRegistryClient registry =
                new HttpDdcServiceRegistryClient(properties, redisson);
        MockRpcGateway gateway = new MockRpcGateway(
                registry,
                properties.getEnv(),
                "mock-gateway:" + ProcessHandle.current().pid(),
                new MockGatewayProperties(
                        "egon-internal-rpc-gateway",
                        "default",
                        "1.0.0",
                        "127.0.0.1",
                        Integer.parseInt(required(values, "gateway.port")),
                        "127.0.0.1",
                        15,
                        3
                ),
                List.of(
                        EchoServiceGrpc.getEchoMethod().getFullMethodName()
                )
        );
        CountDownLatch shutdown = new CountDownLatch(1);
        AtomicBoolean closed = new AtomicBoolean();
        Runnable cleanup = () -> {
            if (closed.compareAndSet(false, true)) {
                gateway.close();
                registry.close();
                redisson.shutdown();
            }
            shutdown.countDown();
        };
        Runtime.getRuntime().addShutdownHook(new Thread(
                cleanup,
                "rpc-mock-gateway-shutdown"
        ));
        try {
            gateway.start();
            System.out.printf(
                    "RPC_MOCK_GATEWAY_READY port=%d%n",
                    gateway.port()
            );
            shutdown.await();
        } finally {
            cleanup.run();
        }
    }

    private static DdcProperties properties(Map<String, String> values) {
        DdcProperties properties = new DdcProperties();
        properties.getAdmin().setEndpoint(
                required(values, "ddc.endpoint")
        );
        properties.getRedis().setHost(
                required(values, "ddc.redis.host")
        );
        properties.getRedis().setPort(Integer.parseInt(
                required(values, "ddc.redis.port")
        ));
        properties.getRedis().setPassword(
                values.get("ddc.redis.password")
        );
        properties.setEnv(required(values, "ddc.env"));
        properties.setNamespace(required(values, "ddc.namespace"));
        properties.getRegistry().setReconcileIntervalSeconds(1);
        return properties;
    }

    private static RedissonClient redisson(DdcProperties properties) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(
                        "redis://"
                                + properties.getRedis().getHost()
                                + ":"
                                + properties.getRedis().getPort()
                );
        String password = properties.getRedis().getPassword();
        if (password != null && !password.isBlank()) {
            config.useSingleServer().setPassword(password);
        }
        return Redisson.create(config);
    }

    private static Map<String, String> parse(String[] arguments) {
        Map<String, String> values = new LinkedHashMap<>();
        Arrays.stream(arguments).forEach(argument -> {
            if (!argument.startsWith("--") || !argument.contains("=")) {
                throw new IllegalArgumentException(
                        "invalid process argument"
                );
            }
            int separator = argument.indexOf('=');
            values.put(
                    argument.substring(2, separator),
                    argument.substring(separator + 1)
            );
        });
        return values;
    }

    private static String required(
            Map<String, String> values,
            String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "missing process argument " + name
            );
        }
        return value;
    }
}
