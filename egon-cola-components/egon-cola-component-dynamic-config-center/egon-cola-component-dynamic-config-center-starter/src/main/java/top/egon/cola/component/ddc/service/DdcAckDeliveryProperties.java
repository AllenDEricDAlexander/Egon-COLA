package top.egon.cola.component.ddc.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "egon.cola.component.ddc.ack-delivery",
        ignoreInvalidFields = true
)
public class DdcAckDeliveryProperties {

    private int queueCapacity = 1024;

    private int maxAttempts = 4;

    private Duration initialBackoff = Duration.ofMillis(100);

    private Duration maxBackoff = Duration.ofSeconds(5);

    private double jitter = 0.2;

    private Duration shutdownWait = Duration.ofSeconds(2);

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public double getJitter() {
        return jitter;
    }

    public void setJitter(double jitter) {
        this.jitter = jitter;
    }

    public Duration getShutdownWait() {
        return shutdownWait;
    }

    public void setShutdownWait(Duration shutdownWait) {
        this.shutdownWait = shutdownWait;
    }
}
