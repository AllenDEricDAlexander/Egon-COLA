package top.egon.cola.component.common.mybatis.extension;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.support.TestBusinessMapper;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessService;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Java-level contracts for the enhanced official Service method families.
 * Real SQL interception is intentionally covered by the later H2 step.
 */
class EgonColaServiceImplTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    private TestTenantIdProvider tenantIdProvider;
    private TestBusinessMapper mapper;
    private TestBusinessService service;

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @BeforeEach
    void setUp() {
        tenantIdProvider = new TestTenantIdProvider();
        tenantIdProvider.set(9L);
        EgonColaModelValidationUtils modelValidationUtils = new EgonColaModelValidationUtils(
                new ValidationUtils(VALIDATOR_FACTORY.getValidator()), tenantIdProvider);
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        properties.getBatch().setMaxChunkSize(2);
        properties.getBatch().setMaxCollectionSize(2);
        service = new TestBusinessService(modelValidationUtils, tenantIdProvider, properties);
        mapper = mock(TestBusinessMapper.class);
        service.setMapperForTest(mapper);
    }

    @Test
    void enhancedServiceImplementationIsPublished() throws ClassNotFoundException {
        assertInstanceOf(EgonColaIService.class, service);
        assertEquals(EgonColaServiceImpl.class, Class.forName(
                "top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl"));
    }

    @Test
    void officialReadAndWriteResultsRetainTheirUpstreamShapes() {
        TestBusinessModel row = new TestBusinessModel().businessValues("saved", "payload");
        when(mapper.insert(any(TestBusinessModel.class))).thenReturn(1);
        when(mapper.selectById(101L)).thenReturn(row);
        when(mapper.selectList(any())).thenReturn(List.of(row));
        when(mapper.selectCount(any())).thenReturn(1L);

        assertTrue(service.save(new TestBusinessModel().businessValues("saved", null)));
        assertSame(row, service.getById(101L));
        assertSame(row, service.getOptById(101L).orElseThrow());
        assertEquals(List.of(row), service.list());
        assertEquals(1L, service.count());
        verify(mapper).insert(any(TestBusinessModel.class));
        verify(mapper, times(2)).selectById(101L);
        verify(mapper, never()).selectById(null);
    }

    @Test
    void missingTenantAndInvalidBusinessModelFailBeforeMapperInvocation() {
        tenantIdProvider.clear();
        assertThrows(IllegalStateException.class, service::list);
        verifyNoInteractions(mapper);

        tenantIdProvider.set(9L);
        assertThrows(ConstraintViolationException.class,
                () -> service.save(new TestBusinessModel().businessValues("", null)));
        verifyNoInteractions(mapper);
    }

    @Test
    void writesRequireIdsAndNonEmptyBusinessPredicates() {
        TestBusinessModel noId = new TestBusinessModel().businessValues("valid", null);
        assertThrows(IllegalArgumentException.class, () -> service.updateById(noId));
        assertThrows(IllegalArgumentException.class, () -> service.removeById(noId));
        assertThrows(IllegalArgumentException.class,
                () -> service.update(new QueryWrapper<>()));
        assertThrows(IllegalArgumentException.class,
                () -> service.remove(new QueryWrapper<>()));
        verifyNoInteractions(mapper);
    }

    @Test
    void batchAndPageBoundsFailBeforeExecutionAndEmptyCollectionsKeepOfficialFalseResult() {
        TestBusinessModel valid = new TestBusinessModel().businessValues("valid", null);
        assertThrows(IllegalArgumentException.class,
                () -> service.saveBatch(List.of(valid), 3));
        assertThrows(ConstraintViolationException.class,
                () -> service.saveBatch(List.of(new TestBusinessModel().businessValues("", null)), 1));
        assertFalse(service.saveBatch(List.of(), 1));
        assertThrows(IllegalArgumentException.class,
                () -> service.list(new Page<>(1, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.list(new Page<>(1, 501)));
        verifyNoInteractions(mapper);
    }

    @Test
    void transactionalFamiliesDeclareExceptionRollback() throws NoSuchMethodException {
        assertRollback("saveBatch", Collection.class, int.class);
        assertRollback("saveOrUpdateBatch", Collection.class, int.class);
        assertRollback("updateBatchById", Collection.class, int.class);
        assertRollback("removeByIds", Collection.class);
        assertRollback("removeBatchByIds", Collection.class);
    }

    private void assertRollback(String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Transactional transactional = EgonColaServiceImpl.class
                .getMethod(methodName, parameterTypes)
                .getAnnotation(Transactional.class);
        assertTrue(transactional != null, methodName + " must be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }
}
