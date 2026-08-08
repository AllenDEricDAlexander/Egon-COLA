package top.egon.cola.component.ddc.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * DDC 发布确认异步投递配置。
 * Configuration for asynchronous DDC publication acknowledgment delivery.
 */
@ConfigurationProperties(
        prefix = "egon.cola.component.ddc.ack-delivery",
        ignoreInvalidFields = true
)
public class DdcAckDeliveryProperties {

    /**
     * 待投递确认的最大队列容量。 Maximum queue capacity for pending acknowledgments.
     */
    private int queueCapacity = 1024;

    /**
     * 单个确认允许的最大投递尝试次数。 Maximum delivery attempts allowed for one acknowledgment.
     */
    private int maxAttempts = 4;

    /**
     * 首次重试的退避时长。 Backoff duration before the first retry.
     */
    private Duration initialBackoff = Duration.ofMillis(100);

    /**
     * 重试退避时长上限。 Maximum retry backoff duration.
     */
    private Duration maxBackoff = Duration.ofSeconds(5);

    /**
     * 退避时长的随机抖动比例。 Random jitter ratio applied to backoff durations.
     */
    private double jitter = 0.2;

    /**
     * 停止时等待投递线程结束的最长时长。 Maximum wait for the delivery thread during shutdown.
     */
    private Duration shutdownWait = Duration.ofSeconds(2);

    /**
     * 获取待投递确认队列容量。
     * Returns the pending acknowledgment queue capacity.
     *
     * @return 队列容量; queue capacity
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * 设置待投递确认队列容量。
     * Sets the pending acknowledgment queue capacity.
     *
     * @param queueCapacity 队列容量; queue capacity
     */
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * 获取最大投递尝试次数。
     * Returns the maximum delivery attempt count.
     *
     * @return 最大尝试次数; maximum attempt count
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * 设置最大投递尝试次数。
     * Sets the maximum delivery attempt count.
     *
     * @param maxAttempts 最大尝试次数; maximum attempt count
     */
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * 获取首次重试退避时长。
     * Returns the initial retry backoff.
     *
     * @return 首次退避时长; initial backoff
     */
    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    /**
     * 设置首次重试退避时长。
     * Sets the initial retry backoff.
     *
     * @param initialBackoff 首次退避时长; initial backoff
     */
    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    /**
     * 获取重试退避时长上限。
     * Returns the maximum retry backoff.
     *
     * @return 最大退避时长; maximum backoff
     */
    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    /**
     * 设置重试退避时长上限。
     * Sets the maximum retry backoff.
     *
     * @param maxBackoff 最大退避时长; maximum backoff
     */
    public void setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    /**
     * 获取退避随机抖动比例。
     * Returns the retry backoff jitter ratio.
     *
     * @return 抖动比例; jitter ratio
     */
    public double getJitter() {
        return jitter;
    }

    /**
     * 设置退避随机抖动比例。
     * Sets the retry backoff jitter ratio.
     *
     * @param jitter 抖动比例; jitter ratio
     */
    public void setJitter(double jitter) {
        this.jitter = jitter;
    }

    /**
     * 获取停止等待时长。
     * Returns the shutdown wait duration.
     *
     * @return 停止等待时长; shutdown wait duration
     */
    public Duration getShutdownWait() {
        return shutdownWait;
    }

    /**
     * 设置停止等待时长。
     * Sets the shutdown wait duration.
     *
     * @param shutdownWait 停止等待时长; shutdown wait duration
     */
    public void setShutdownWait(Duration shutdownWait) {
        this.shutdownWait = shutdownWait;
    }
}
