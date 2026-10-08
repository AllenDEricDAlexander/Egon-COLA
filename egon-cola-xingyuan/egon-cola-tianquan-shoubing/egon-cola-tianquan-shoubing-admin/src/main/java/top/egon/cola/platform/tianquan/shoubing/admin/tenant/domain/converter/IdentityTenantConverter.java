package top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantPO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IdentityTenantConverter extends BaseConverter<IdentityTenantPO, IdentityTenantBO> {

    ObjectMapper SETTINGS_MAPPER = new ObjectMapper();

    @Override
    @Mapping(target = "settings", source = "settings", qualifiedByName = "settingsToString")
    @Mapping(target = "createdBy", source = "createUserId")
    @Mapping(target = "updatedBy", source = "updateUserId")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    IdentityTenantBO toTarget(IdentityTenantPO source);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", source = "createdBy")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateUserId", source = "updatedBy")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "settings", source = "settings", qualifiedByName = "settingsToJsonNode")
    IdentityTenantPO toSource(IdentityTenantBO target);

    @Named("settingsToString")
    default String settingsToString(JsonNode settings) {
        return settings == null ? "{}" : settings.toString();
    }

    @Named("settingsToJsonNode")
    default JsonNode settingsToJsonNode(String settings) {
        String value = settings == null || settings.isBlank() ? "{}" : settings.trim();
        try {
            JsonNode parsed = SETTINGS_MAPPER.readTree(value);
            if (parsed == null || !parsed.isObject()
                    || value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65_536) {
                throw new IllegalArgumentException("tenant settings are invalid");
            }
            return parsed;
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("tenant settings are invalid", failure);
        }
    }
}
