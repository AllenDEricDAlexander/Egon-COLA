package top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Preserves the PostgreSQL jsonb parameter type when ShardingSphere replays JDBC bindings. */
public final class OAuthJsonbTypeHandler extends JacksonTypeHandler {

    public OAuthJsonbTypeHandler(Class<?> type) {
        super(type);
    }

    public OAuthJsonbTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, Object parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject json = new PGobject();
        json.setType("jsonb");
        json.setValue(toJson(parameter));
        statement.setObject(index, json);
    }
}
