package top.egon.cola.component.dtp.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @ClassName: DynamicThreadPoolAutoProperties
 * @description: 动态线程池配置
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午8:21
 * @Version: 1.0
 */
@ConfigurationProperties(prefix = "egon.cola.component.dtp", ignoreInvalidFields = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class DynamicThreadPoolAutoProperties {

    private boolean enabled = true;

    private String appName;

    private String instanceId;

    private Registry registry = new Registry();

    private Report report = new Report();

    private Trace trace = new Trace();

    private Virtual virtual = new Virtual();

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Registry {

        private String type = "redis";

        private Redis redis = new Redis();

    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Redis {

        private String host = "127.0.0.1";

        private int port = 6379;

        private String password;

        private int database = 0;

        private int poolSize = 64;

        private int minIdleSize = 10;

        private int idleTimeout = 10000;

        private int connectTimeout = 10000;

        private int retryAttempts = 3;

        private int retryInterval = 1000;

        private int pingInterval = 0;

        private boolean keepAlive = true;

    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Report {

        private boolean enabled = true;

        private Duration interval = Duration.ofSeconds(20);

    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Trace {

        private boolean enabled = true;

        private boolean mdcEnabled = true;

        private String traceIdKey = "traceId";

        private String requestIdKey = "requestId";

    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Virtual {

        private boolean enabled = true;

        private int defaultConcurrencyLimit = 500;

    }

}
