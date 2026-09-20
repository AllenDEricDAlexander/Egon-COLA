package top.egon.cola.component.common.mybatis.model;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

/** MyBatis-Plus adapter for the process-wide distributed Long ID strategy. */
@Slf4j
public final class EgonColaIdentifierGenerator implements IdentifierGenerator {

    @Override
    public Long nextId(Object entity) {
        if (!(entity instanceof EgonModel<?>)) {
            throw new IllegalArgumentException("EGON_MODEL_REQUIRED");
        }
        long id = SnowflakeIdGenerator.nextLongId();
        if (id <= 0) {
            throw new IllegalStateException("DISTRIBUTED_ID_INVALID");
        }
        return id;
    }
}
