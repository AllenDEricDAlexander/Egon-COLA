package top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo;

import top.egon.cola.archetype.source.webopen.infrastructure.teaching.dao.GradeDAO;
import top.egon.cola.archetype.source.webopen.infrastructure.teaching.po.GradePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import java.util.Collection;
import java.util.List;
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;

/** Logical persistence boundary; query implementations remain explicit Mapper SQL. */
@Slf4j
@Validated
@Repository("gradeRepository")
@RequiredArgsConstructor
@CacheConfig(cacheNames = "GradePO")
public class GradeRepository extends EgonColaRepository<GradeDAO, GradePO> {
    @Getter
    @Qualifier("gradeDAO")
    private final GradeDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public GradePO selectByCode(@NotBlank String code) {
        return getBaseMapper().selectByCode(code);
    }

    public long countByCode(@NotBlank String code) {
        return getBaseMapper().countByCode(code);
    }

    /**
     * 通过 Spring 代理调用；sync 模式缓存短 TTL 空值，不与 unless 混用。
     */
    @Cacheable(key = "T(org.slf4j.MDC).get('tenantId') + ':' + #p0", sync = true)
    public GradePO findCachedById(@NotNull @Positive Long id) {
        return getById(id);
    }

    /**
     * 缓存写入口必须与读入口使用相同区域和 Key；普通 CRUD 不再隐式失效缓存。
     */
    @CacheEvict(key = "T(org.slf4j.MDC).get('tenantId') + ':' + #p0.id", condition = "#result")
    public boolean updateCachedById(@NotNull GradePO entity) {
        return updateById(entity);
    }
}
