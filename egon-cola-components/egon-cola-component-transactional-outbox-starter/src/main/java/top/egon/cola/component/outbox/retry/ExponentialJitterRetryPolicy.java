package top.egon.cola.component.outbox.retry;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.function.DoubleSupplier;

public class ExponentialJitterRetryPolicy implements OutboxRetryPolicy {

    private static final BigDecimal NANOS_PER_SECOND = BigDecimal.valueOf(1_000_000_000L);
    private static final BigInteger NANOS_PER_SECOND_INTEGER = BigInteger.valueOf(1_000_000_000L);
    private static final MathContext CALCULATION_CONTEXT = MathContext.DECIMAL128;

    private final BigDecimal initialNanos;
    private final BigDecimal multiplier;
    private final BigDecimal maximumNanos;
    private final BigDecimal jitter;
    private final DoubleSupplier random;

    public ExponentialJitterRetryPolicy(
            Duration initialDelay,
            double multiplier,
            Duration maxDelay,
            double jitter,
            DoubleSupplier random
    ) {
        this.initialNanos = nanos(initialDelay);
        this.multiplier = BigDecimal.valueOf(multiplier);
        this.maximumNanos = nanos(maxDelay);
        this.jitter = BigDecimal.valueOf(jitter);
        this.random = random;
    }

    @Override
    public Duration nextDelay(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be one-based");
        }
        BigDecimal exponential = exponentialDelay(attempt);
        BigDecimal randomValue = BigDecimal.valueOf(random.getAsDouble());
        BigDecimal jitterFactor = BigDecimal.ONE.add(
                randomValue.multiply(BigDecimal.valueOf(2), CALCULATION_CONTEXT)
                        .subtract(BigDecimal.ONE)
                        .multiply(jitter, CALCULATION_CONTEXT),
                CALCULATION_CONTEXT
        );
        BigDecimal jittered = exponential.multiply(jitterFactor, CALCULATION_CONTEXT);
        BigDecimal bounded = jittered.max(BigDecimal.ZERO).min(maximumNanos);
        return duration(bounded);
    }

    private BigDecimal exponentialDelay(int attempt) {
        if (initialNanos.compareTo(maximumNanos) >= 0) {
            return maximumNanos;
        }
        long exponent = (long) attempt - 1;
        BigDecimal cap = maximumNanos.divide(initialNanos, CALCULATION_CONTEXT);
        BigDecimal factor = cappedPower(multiplier, exponent, cap);
        if (factor.compareTo(cap) >= 0) {
            return maximumNanos;
        }
        return initialNanos.multiply(factor, CALCULATION_CONTEXT).min(maximumNanos);
    }

    private BigDecimal cappedPower(BigDecimal base, long exponent, BigDecimal cap) {
        BigDecimal result = BigDecimal.ONE;
        BigDecimal factor = base.min(cap);
        long remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1L) == 1L) {
                result = result.multiply(factor, CALCULATION_CONTEXT);
                if (result.compareTo(cap) >= 0) {
                    return cap;
                }
            }
            remaining >>>= 1;
            if (remaining > 0) {
                factor = factor.multiply(factor, CALCULATION_CONTEXT).min(cap);
            }
        }
        return result;
    }

    private static BigDecimal nanos(Duration duration) {
        return BigDecimal.valueOf(duration.getSeconds())
                .multiply(NANOS_PER_SECOND)
                .add(BigDecimal.valueOf(duration.getNano()));
    }

    private static Duration duration(BigDecimal nanoseconds) {
        BigInteger totalNanos = nanoseconds.setScale(0, RoundingMode.DOWN).toBigIntegerExact();
        BigInteger[] parts = totalNanos.divideAndRemainder(NANOS_PER_SECOND_INTEGER);
        return Duration.ofSeconds(parts[0].longValueExact(), parts[1].longValueExact());
    }
}
