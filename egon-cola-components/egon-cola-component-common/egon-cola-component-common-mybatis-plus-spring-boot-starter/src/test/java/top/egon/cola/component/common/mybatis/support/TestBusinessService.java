package top.egon.cola.component.common.mybatis.support;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

/**
 * Test-only technical Service extension used by the unit and integration
 * contracts. It contains no business rules or CRUD overrides.
 */
@RequiredArgsConstructor
public class TestBusinessService extends EgonColaServiceImpl<TestBusinessMapper, TestBusinessModel> {

    @Getter(AccessLevel.PROTECTED)
    private final EgonColaModelValidationUtils modelValidationUtils;

    @Getter(AccessLevel.PROTECTED)
    private final EgonColaTenantIdProvider tenantIdProvider;

    @Getter(AccessLevel.PROTECTED)
    private final EgonColaMybatisPlusProperties properties;

    public void setMapperForTest(TestBusinessMapper mapper) {
        this.baseMapper = mapper;
    }
}
