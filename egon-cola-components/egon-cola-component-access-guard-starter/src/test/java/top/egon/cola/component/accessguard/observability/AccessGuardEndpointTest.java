package top.egon.cola.component.accessguard.observability;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.plan.DefaultGuardPlanResolver;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.core.plan.GuardPlanValidator;
import top.egon.cola.component.accessguard.core.plan.PropertiesGuardPlanSource;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardEndpointTest {

    private static final ValidationUtils VALIDATION_UTILS =
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void endpointReturnsOnlyBoundedRuleAndHealthData() {
        AccessGuardProperties properties = new AccessGuardProperties();
        AccessGuardProperties.Rule rule = new AccessGuardProperties.Rule();
        rule.getDenyList().setEnabled(true);
        rule.getAllowList().setEnabled(true);
        rule.getRejection().setFallbackMethod("drawFallback");
        properties.setRules(Map.of("draw", rule));
        properties.getKey().setHmacSecret("raw-key");
        properties.getKey().setHeaders(List.of("Authorization"));
        DefaultGuardPlanResolver resolver = new DefaultGuardPlanResolver(
                List.of(new PropertiesGuardPlanSource(properties)), new GuardPlanValidator(VALIDATION_UTILS));
        AccessGuardEndpoint endpoint = new AccessGuardEndpoint(properties, resolver, () -> 2, () -> 3);

        Map<String, Object> response = endpoint.accessguard();

        assertThat(response.toString())
                .contains("draw", "planVersion", "properties", "deny-list", "allow-list", "LOCAL", "health")
                .doesNotContain("raw-key", "Authorization", "drawFallback", "user-1", "fallback arguments");
    }
}
