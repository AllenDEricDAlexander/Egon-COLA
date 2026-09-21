package top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo;

import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.dao.CourseDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.po.CoursePO;
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

/** Logical persistence boundary; query implementations remain explicit Mapper SQL. */
@Slf4j
@Validated
@Repository("courseRepository")
@RequiredArgsConstructor
@CacheConfig(cacheNames = "CoursePO")
public class CourseRepository extends EgonColaRepository<CourseDAO, CoursePO> {
    @Getter
    @Qualifier("courseDAO")
    private final CourseDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<CoursePO> selectByCourseCode(@NotBlank String courseCode) {
        return getBaseMapper().selectByCourseCode(courseCode);
    }

    /**
     * 通过 Spring 代理调用；sync 模式缓存短 TTL 空值，不与 unless 混用。
     */
    @Cacheable(key = "T(org.slf4j.MDC).get('tenantId') + ':' + #p0", sync = true)
    public CoursePO findCachedById(@NotNull @Positive Long id) {
        return getById(id);
    }

    /**
     * 缓存写入口必须与读入口使用相同区域和 Key；普通 CRUD 不再隐式失效缓存。
     */
    @CacheEvict(key = "T(org.slf4j.MDC).get('tenantId') + ':' + #p0.id", condition = "#result")
    public boolean updateCachedById(@NotNull CoursePO entity) {
        return updateById(entity);
    }
}
