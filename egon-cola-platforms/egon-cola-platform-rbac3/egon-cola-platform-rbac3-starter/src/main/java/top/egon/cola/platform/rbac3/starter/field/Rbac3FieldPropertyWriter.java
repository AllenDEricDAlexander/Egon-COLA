package top.egon.cola.platform.rbac3.starter.field;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;

import java.util.Locale;

final class Rbac3FieldPropertyWriter extends BeanPropertyWriter {

    private final RBACFieldResource resource;
    private final Sensitive staticSensitive;
    private final CurrentRbac3User currentUser;
    private final SensitiveStrategyRegistry strategyRegistry;

    Rbac3FieldPropertyWriter(
            BeanPropertyWriter source,
            RBACFieldResource resource,
            Sensitive staticSensitive,
            CurrentRbac3User currentUser,
            SensitiveStrategyRegistry strategyRegistry) {
        super(source);
        this.resource = resource;
        this.staticSensitive = staticSensitive;
        this.currentUser = currentUser;
        this.strategyRegistry = strategyRegistry;
        validate(resource);
    }

    @Override
    public void serializeAsField(
            Object bean,
            JsonGenerator generator,
            SerializerProvider provider) throws Exception {
        FieldAccess access = resolveAccess();
        if (access.level() == FieldAccessLevel.NONE) {
            writeNull(generator);
            return;
        }
        if (access.level() != FieldAccessLevel.MASKED_READ) {
            if (staticSensitive == null) {
                super.serializeAsField(bean, generator, provider);
            } else {
                super.serializeAsField(
                        bean,
                        new StaticMaskingJsonGenerator(generator),
                        provider);
            }
            return;
        }

        Object value;
        try {
            value = get(bean);
        } catch (Exception error) {
            writeNull(generator);
            return;
        }
        if (value == null) {
            writeNull(generator);
            return;
        }
        if (!(value instanceof String stringValue)) {
            writeNull(generator);
            return;
        }

        String masked;
        try {
            SensitiveType type = access.maskingStrategy();
            if (type == null) {
                writeNull(generator);
                return;
            }
            masked = strategyRegistry.mask(type, stringValue);
        } catch (Exception error) {
            writeNull(generator);
            return;
        }

        generator.writeFieldName(getSerializedName());
        if (masked == null) {
            generator.writeNull();
        } else {
            generator.writeString(masked);
        }
    }

    private FieldAccess resolveAccess() {
        Rbac3UserDetails details = currentUser.current().orElse(null);
        if (details == null) {
            return FieldAccess.none();
        }
        String key = resource.permission() + ':'
                + details.snapshot().systemCode() + ':' + resource.resourceCode();
        FieldPolicyDecision policy = details.fieldPolicies().get(key);
        if (policy == null || policy.decision() != Decision.ALLOW) {
            return FieldAccess.none();
        }
        FieldPolicyDecision.FieldAccess configured = policy.fields().get(resource.code());
        if (configured == null) {
            return FieldAccess.none();
        }
        SensitiveType strategy = configured.maskingStrategy() == null
                ? resource.maskingStrategy()
                : parseStrategy(configured.maskingStrategy());
        return new FieldAccess(configured.level(), strategy);
    }

    private SensitiveType parseStrategy(String value) {
        try {
            return SensitiveType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException error) {
            return null;
        }
    }

    private void writeNull(JsonGenerator generator) throws Exception {
        generator.writeFieldName(getSerializedName());
        generator.writeNull();
    }

    private class StaticMaskingJsonGenerator extends JsonGeneratorDelegate {

        StaticMaskingJsonGenerator(JsonGenerator delegate) {
            super(delegate);
        }

        @Override
        public void writeString(String value) throws java.io.IOException {
            try {
                super.writeString(strategyRegistry.mask(staticSensitive.type(), value));
            } catch (RuntimeException error) {
                super.writeNull();
            }
        }

        @Override
        public void writeString(char[] text, int offset, int length)
                throws java.io.IOException {
            writeString(new String(text, offset, length));
        }

        @Override
        public void writeString(com.fasterxml.jackson.core.SerializableString value)
                throws java.io.IOException {
            writeString(value.getValue());
        }
    }

    private static void validate(RBACFieldResource resource) {
        required(resource.code(), "code");
        required(resource.name(), "name");
        required(resource.resourceCode(), "resourceCode");
        required(resource.permission(), "permission");
    }

    private static void required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "@RBACFieldResource " + name + " must not be blank");
        }
    }

    private record FieldAccess(FieldAccessLevel level, SensitiveType maskingStrategy) {

        static FieldAccess none() {
            return new FieldAccess(FieldAccessLevel.NONE, null);
        }
    }
}
