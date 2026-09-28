package top.egon.cola.component.tianshu.admin.model.convertor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class DdcRuntimeMetadataTypeHandler extends BaseTypeHandler<Map<String, String>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() { };

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, Map<String, String> value,
                                    JdbcType jdbcType) throws SQLException {
        try {
            statement.setString(index, MAPPER.writeValueAsString(value));
        } catch (JsonProcessingException failure) {
            throw new SQLException("TIANSHU_RUNTIME_METADATA_SERIALIZATION_FAILED", failure);
        }
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return parse(resultSet.getString(columnName));
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return parse(resultSet.getString(columnIndex));
    }

    @Override
    public Map<String, String> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return parse(statement.getString(columnIndex));
    }

    private Map<String, String> parse(String value) throws SQLException {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = MAPPER.readValue(value, MAP_TYPE);
            return parsed == null ? Map.of() : Map.copyOf(parsed);
        } catch (JsonProcessingException failure) {
            throw new SQLException("TIANSHU_RUNTIME_METADATA_DESERIALIZATION_FAILED", failure);
        }
    }
}
