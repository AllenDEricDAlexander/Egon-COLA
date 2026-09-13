package top.egon.cola.component.common.mybatis.model;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

/** MyBatis-Plus adapter for the application's existing distributed Long ID strategy. */
@Slf4j
@RequiredArgsConstructor
public final class EgonColaIdentifierGenerator implements IdentifierGenerator {

    @Qualifier("snowflakeIdGenerator")
    private final LongIdGenerator delegate;

    @Override
    public Long nextId(Object entity) {
        if (!(entity instanceof EgonModel<?>)) {
            throw new IllegalArgumentException("EGON_MODEL_REQUIRED");
        }
        long id = delegate.nextLongId();
        if (id <= 0) {
            throw new IllegalStateException("DISTRIBUTED_ID_INVALID");
        }
        return id;
    }
}
