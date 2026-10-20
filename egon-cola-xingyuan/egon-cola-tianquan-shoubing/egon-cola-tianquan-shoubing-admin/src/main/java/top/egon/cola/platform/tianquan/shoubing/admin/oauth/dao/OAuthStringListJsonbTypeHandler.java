package top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class OAuthStringListJsonbTypeHandler extends BaseTypeHandler<List<String>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(
            PreparedStatement statement,
            int index,
            List<String> values,
            JdbcType jdbcType) throws SQLException {
        for (String value : values) {
            requireText(value);
        }
        try {
            PGobject json = new PGobject();
            json.setType("jsonb");
            json.setValue(OBJECT_MAPPER.writeValueAsString(values));
            statement.setObject(index, json);
        } catch (JsonProcessingException failure) {
            throw new SQLException("OAUTH_CLIENT_JSON_ARRAY_ENCODE_FAILED", failure);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return parse(resultSet.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return parse(resultSet.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return parse(statement.getString(columnIndex));
    }

    private static List<String> parse(String json) throws SQLException {
        if (json == null) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root == null || !root.isArray()) {
                throw new SQLException("OAUTH_CLIENT_JSON_ARRAY_REQUIRED");
            }
            List<String> values = new ArrayList<>(root.size());
            for (JsonNode item : root) {
                if (!item.isTextual()) {
                    throw new SQLException("OAUTH_CLIENT_JSON_ARRAY_TEXT_REQUIRED");
                }
                String value = item.textValue();
                requireText(value);
                values.add(value);
            }
            return List.copyOf(values);
        } catch (JsonProcessingException failure) {
            throw new SQLException("OAUTH_CLIENT_JSON_ARRAY_INVALID", failure);
        } catch (IllegalArgumentException failure) {
            throw new SQLException("OAUTH_CLIENT_JSON_ARRAY_TEXT_REQUIRED", failure);
        }
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("OAUTH_CLIENT_JSON_ARRAY_TEXT_REQUIRED");
        }
    }
}
