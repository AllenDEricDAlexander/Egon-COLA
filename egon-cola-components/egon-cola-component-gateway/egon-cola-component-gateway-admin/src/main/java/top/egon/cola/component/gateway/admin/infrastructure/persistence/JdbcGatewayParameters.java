package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;

final class JdbcGatewayParameters {

    private JdbcGatewayParameters() {
    }

    static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
