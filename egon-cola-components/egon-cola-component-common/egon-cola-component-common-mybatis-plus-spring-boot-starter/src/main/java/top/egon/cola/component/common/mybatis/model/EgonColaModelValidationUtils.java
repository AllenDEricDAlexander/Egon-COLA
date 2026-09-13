package top.egon.cola.component.common.mybatis.model;

import jakarta.validation.groups.Default;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import java.lang.reflect.Field;
import java.util.Set;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;


/**
 * Repository-specific validation facade for EgonModel operations.
 */
@Slf4j
@RequiredArgsConstructor
public final class EgonColaModelValidationUtils {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;
    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;

    public <M extends EgonModel<M>> M validateBusiness(
            M model, EgonColaModelValidationGroups.Operation operation) {
        M checkedModel = requireModel(model);
        validateMetadataOwnership(checkedModel.getClass());
        EgonColaModelValidationGroups.Operation checkedOperation = requireOperation(operation);
        validationUtils.validate(checkedModel, Default.class, checkedOperation.group());
        return checkedModel;
    }

    public <M extends EgonModel<M>> M validate(
            M model, EgonColaModelValidationGroups.Operation operation) {
        M checkedModel = requireModel(model);
        validateMetadataOwnership(checkedModel.getClass());
        EgonColaModelValidationGroups.Operation checkedOperation = requireOperation(operation);
        if (checkedOperation == EgonColaModelValidationGroups.Operation.LOADED) {
            validationUtils.validate(checkedModel, Default.class,
                    EgonColaModelValidationGroups.Persisted.class);
        } else {
            validationUtils.validate(checkedModel, Default.class, checkedOperation.group(),
                    EgonColaModelValidationGroups.Persisted.class);
        }
        Long currentTenantId = tenantIdProvider.currentTenantId();
        if (currentTenantId == null || !currentTenantId.equals(checkedModel.getTenantId())) {
            throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
        }
        return checkedModel;
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
}
