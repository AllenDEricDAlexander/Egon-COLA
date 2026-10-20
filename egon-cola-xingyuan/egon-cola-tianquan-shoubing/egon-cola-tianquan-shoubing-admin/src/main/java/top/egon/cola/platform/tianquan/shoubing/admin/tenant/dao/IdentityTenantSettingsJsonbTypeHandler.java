package top.egon.cola.platform.tianquan.shoubing.admin.tenant.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(JsonNode.class)
@MappedJdbcTypes(JdbcType.OTHER)
public final class IdentityTenantSettingsJsonbTypeHandler extends BaseTypeHandler<JsonNode> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(
            PreparedStatement statement,
            int index,
            JsonNode value,
            JdbcType jdbcType) throws SQLException {
        requireObject(value);
        PGobject json = new PGobject();
        json.setType("jsonb");
        json.setValue(value.toString());
        statement.setObject(index, json);
    }

    @Override
    public JsonNode getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return parse(resultSet.getString(columnName));
    }

    @Override
    public JsonNode getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return parse(resultSet.getString(columnIndex));
    }

    @Override
    public JsonNode getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return parse(statement.getString(columnIndex));
    }

    private static JsonNode parse(String value) throws SQLException {
        if (value == null) {
            return null;
        }
        try {
            JsonNode result = OBJECT_MAPPER.readTree(value);
            requireObject(result);
            return result;
        } catch (JsonProcessingException failure) {
            throw new SQLException("IDENTITY_TENANT_SETTINGS_JSON_INVALID", failure);
        }
    }

    private static void requireObject(JsonNode value) throws SQLException {
        if (value == null || !value.isObject()
                || value.toString().getBytes(StandardCharsets.UTF_8).length > 65_536) {
            throw new SQLException("IDENTITY_TENANT_SETTINGS_OBJECT_REQUIRED");
        }
    }
}
