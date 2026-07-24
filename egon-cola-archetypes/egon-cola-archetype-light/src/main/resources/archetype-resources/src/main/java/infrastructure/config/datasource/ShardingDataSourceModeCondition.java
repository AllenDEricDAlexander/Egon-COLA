package ${package}.infrastructure.config.datasource;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Activates ShardingSphere for every non-single data source mode.
 */
public final class ShardingDataSourceModeCondition implements Condition {

    @Override
    public boolean matches(
            ConditionContext context,
            AnnotatedTypeMetadata metadata) {
        String mode = context.getEnvironment()
                .getProperty("app.datasource.mode", "SINGLE");
        return !"SINGLE".equalsIgnoreCase(mode.strip());
    }
}
