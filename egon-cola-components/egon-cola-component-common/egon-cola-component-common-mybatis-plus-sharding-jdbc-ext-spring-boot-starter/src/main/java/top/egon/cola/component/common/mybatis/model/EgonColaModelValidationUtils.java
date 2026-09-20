package top.egon.cola.component.common.mybatis.model;

import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;

/**
 * Static repository validation facade for EgonModel operations.
 *
 * <p>The Spring assembled {@link ValidationUtils} is bound once through {@link #initialize} instead of
 * being injected into every consumer. A repeated identical binding is reused, a materially different
 * binding is rejected while its owner is live, and disposal of the owning assembly releases only that
 * binding.</p>
 */
@Slf4j
public final class EgonColaModelValidationUtils {

    private static final Object BIND_MONITOR = new Object();

    private static volatile Binding binding;

    private EgonColaModelValidationUtils() {
    }

    /**
     * Binds the Spring assembled validation facade, or verifies the live binding is the same one.
     *
     * @param validationUtils facade built from the assembled Validator
     * @param owner assembly that owns the binding, reported when a binding conflicts
     * @return the live binding
     * @throws EgonColaMybatisPlusConfigurationException when a different binding is already live
     */
    public static Binding initialize(ValidationUtils validationUtils, Object owner) {
        Objects.requireNonNull(validationUtils, "validationUtils must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
        synchronized (BIND_MONITOR) {
            Binding current = binding;
            if (current == null) {
                binding = new Binding(validationUtils, owner);
                return binding;
            }
            if (current.validationUtils() == validationUtils) {
                return current;
            }
            throw new EgonColaMybatisPlusConfigurationException("MODEL_VALIDATION_BINDING_CONFLICT",
                    new IllegalStateException("Model validation is already bound by a live owner "
                            + current.owner() + "; refusing to rebind the static entry to " + owner));
        }
    }

    /**
     * Returns the live binding, or {@code null} while no assembly owns the static entry.
     *
     * @return the current binding
     */
    public static Binding current() {
        return binding;
    }

    public static <M extends EgonModel<M>> void validateBusiness(
            M model, EgonColaModelValidationGroups.Operation operation) {
        M checkedModel = requireModel(model);
        validateMetadataOwnership(checkedModel.getClass());
        EgonColaModelValidationGroups.Operation checkedOperation = requireOperation(operation);
        boundValidationUtils().validate(checkedModel, Default.class, checkedOperation.group());
    }

    public static <M extends EgonModel<M>> M validate(
            M model, EgonColaModelValidationGroups.Operation operation) {
        M checkedModel = requireModel(model);
        validateMetadataOwnership(checkedModel.getClass());
        EgonColaModelValidationGroups.Operation checkedOperation = requireOperation(operation);
        ValidationUtils validationUtils = boundValidationUtils();
        if (checkedOperation == EgonColaModelValidationGroups.Operation.LOADED) {
            validationUtils.validate(checkedModel, Default.class,
                    EgonColaModelValidationGroups.Persisted.class);
        } else {
            validationUtils.validate(checkedModel, Default.class, checkedOperation.group(),
                    EgonColaModelValidationGroups.Persisted.class);
        }
        if (!EgonColaTenantIdProvider.currentTenantId().equals(checkedModel.getTenantId())) {
            throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
        }
        return checkedModel;
    }

    private static ValidationUtils boundValidationUtils() {
        Binding current = binding;
        if (current == null) {
            throw new EgonColaMybatisPlusConfigurationException("MODEL_VALIDATION_UTILS_NOT_BOUND");
        }
        return current.validationUtils();
    }

    private static void validateMetadataOwnership(Class<?> type) {
        Set<String> fields = Set.of("id", "tenantId", "createUserId", "createTime", "updateUserId", "updateTime", "deletedAt", "version");
        Set<String> columns = Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id", "update_time", "deleted_at", "version");
        for (Class<?> current = type; current != null && current != EgonModel.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                TableField annotation = field.getAnnotation(TableField.class);
                if (fields.contains(field.getName()) || (annotation != null && annotation.exist()
                        && columns.contains(annotation.value().toLowerCase(java.util.Locale.ROOT)))) {
                    throw new IllegalStateException("MODEL_TECHNICAL_FIELD_SHADOWED: " + field.getName());
                }
            }
        }
    }

    private static <M extends EgonModel<M>> M requireModel(M model) {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        return model;
    }

    private static EgonColaModelValidationGroups.Operation requireOperation(
            EgonColaModelValidationGroups.Operation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        return operation;
    }

    /**
     * Live binding of the static model validation entry.
     *
     * @param validationUtils bound Spring assembled facade
     * @param owner assembly that owns the binding
     */
    public record Binding(ValidationUtils validationUtils, Object owner) implements AutoCloseable {

        /**
         * Releases this binding only while it is still the live one.
         */
        @Override
        public void close() {
            synchronized (BIND_MONITOR) {
                if (binding == this) {
                    binding = null;
                }
            }
        }
    }
}
