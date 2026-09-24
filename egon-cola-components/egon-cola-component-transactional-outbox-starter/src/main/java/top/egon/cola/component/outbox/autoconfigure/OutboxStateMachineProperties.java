package top.egon.cola.component.outbox.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Typed execution budgets and opt-in business-event support for the outbox. */
@Data
@Accessors(chain = true)
@Validated
@ConfigurationProperties(prefix = OutboxStateMachineProperties.PREFIX, ignoreUnknownFields = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxStateMachineProperties {

    public static final String PREFIX = "egon.cola.component.transactional-outbox.state-machine";

    @NotNull
    @Builder.Default
    private Duration executionTimeout = Duration.ofMillis(100);

    @NotNull
    @Builder.Default
    private Duration claimEvaluationTimeout = Duration.ofSeconds(1);

    @Valid
    @NotNull
    @Builder.Default
    private BusinessProperties business = new BusinessProperties();

    @AssertTrue(message = "State-machine execution budgets must be positive and ordered")
    public boolean isExecutionBudgetValid() {
        return positive(executionTimeout)
                && positive(claimEvaluationTimeout)
                && executionTimeout.compareTo(claimEvaluationTimeout) <= 0;
    }

    private static boolean positive(Duration value) {
        return value != null && !value.isNegative() && !value.isZero();
    }

    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BusinessProperties {

        @Builder.Default
        private boolean enabled = false;

        @NotNull
        @Builder.Default
        private Duration timeout = Duration.ofSeconds(5);

        @AssertTrue(message = "Business state-machine timeout must be positive")
        public boolean isTimeoutValid() {
            return positive(timeout);
        }
    }
}
