package top.egon.cola.component.common.mybatis.sharding;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EgonColaShardingPropertiesTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void binds_valid_yaml() {
        EgonColaShardingProperties properties = bind(validSource());
        assertThat(VALIDATOR.validate(properties)).isEmpty();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getMode()).isEqualTo(EgonColaShardingProperties.ModeEnum.SHARDING);
        assertThat(properties.getDataSources()).singleElement().satisfies(source -> {
            assertThat(source.driverClassName()).isEqualTo("org.postgresql.Driver");
            assertThat(source.toString()).doesNotContain("secret");
        });
    }

    @Test
    void rejects_enabled_false() {
        Map<String, Object> source = validSource();
        source.put("egon.cola.component.mybatis-plus.sharding.enabled", "false");
        EgonColaShardingProperties properties = bind(source);
        assertThat(VALIDATOR.validate(properties))
                .extracting(ConstraintViolation::getMessage)
                .contains("SHARDING_REQUIRED");
    }

    @Test
    void rejects_non_postgresql_driver() {
        Map<String, Object> source = validSource();
        source.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].driver-class-name",
                "org.h2.Driver");
        Set<ConstraintViolation<EgonColaShardingProperties>> violations = VALIDATOR.validate(bind(source));
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .anyMatch(path -> path.contains("driverClassName"));
    }

    @Test
    void rejects_empty_data_sources() {
        Map<String, Object> source = validSource();
        source.keySet().removeIf(key -> key.contains("data-sources"));
        Set<ConstraintViolation<EgonColaShardingProperties>> violations = VALIDATOR.validate(bind(source));
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("dataSources");
    }

    private static EgonColaShardingProperties bind(Map<String, Object> source) {
        return new Binder(new MapConfigurationPropertySource(source))
                .bind("egon.cola.component.mybatis-plus.sharding", Bindable.of(EgonColaShardingProperties.class))
                .get();
    }

    private static Map<String, Object> validSource() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("egon.cola.component.mybatis-plus.sharding.enabled", "true");
        values.put("egon.cola.component.mybatis-plus.sharding.mode", "SHARDING");
        values.put("egon.cola.component.mybatis-plus.sharding.config-style", "STRATEGY");
        values.put("egon.cola.component.mybatis-plus.sharding.transaction-default-type", "LOCAL");
        values.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].name", "master_data");
        values.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].logical-name", "master_data");
        values.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].role", "PRIMARY");
        values.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].driver-class-name",
                "org.postgresql.Driver");
        values.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].jdbc-url",
                "jdbc:postgresql://localhost/master_data");
        values.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].username", "sa");
        values.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].password", "secret");
        return values;
    }
}
