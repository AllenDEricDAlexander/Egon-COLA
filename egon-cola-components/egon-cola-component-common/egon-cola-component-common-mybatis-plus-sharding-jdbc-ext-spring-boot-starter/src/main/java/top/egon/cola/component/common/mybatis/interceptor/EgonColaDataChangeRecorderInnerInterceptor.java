package top.egon.cola.component.common.mybatis.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.DataChangeRecorderInnerInterceptor;
import lombok.extern.slf4j.Slf4j;

/** Development-only prepare-time diagnostics; this is not a record of a committed transaction. */
@Slf4j(topic = "top.egon.cola.component.common.mybatis.change-summary")
@SuppressWarnings("deprecation")
@Deprecated
public final class EgonColaDataChangeRecorderInnerInterceptor extends DataChangeRecorderInnerInterceptor {

    @Override
    protected void dealOperationResult(OperationResult result) {
        log.debug("operation={}, table={}, recorded={}, costMs={}", result.getOperation(), result.getTableName(),
                result.isRecordStatus(), result.getCost());
    }
}
