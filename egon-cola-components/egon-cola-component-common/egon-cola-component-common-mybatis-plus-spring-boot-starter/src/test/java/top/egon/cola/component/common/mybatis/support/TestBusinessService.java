package top.egon.cola.component.common.mybatis.support;

import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

/**
 * Test-only technical Service extension used by the unit and integration
 * contracts. It contains no business rules or CRUD overrides.
 */
public final class TestBusinessService extends EgonColaServiceImpl<TestBusinessMapper, TestBusinessModel> {

    public TestBusinessService(EgonColaModelValidationUtils modelValidationUtils,
                                EgonColaTenantIdProvider tenantIdProvider,
                                EgonColaMybatisPlusProperties properties) {
        super(modelValidationUtils, tenantIdProvider, properties);
    }

    public void setMapperForTest(TestBusinessMapper mapper) {
        this.baseMapper = mapper;
    }
}
