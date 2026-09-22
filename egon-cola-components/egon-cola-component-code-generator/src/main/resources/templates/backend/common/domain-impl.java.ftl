package [=domainImplPackage];

import [=domainFqn];
import [=domainQueryFqn];
import [=domainServiceFqn];
import [=poFqn];
import [=repoPackage].[=repoType];
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageSlice;

import java.util.ArrayList;
import java.util.List;

@Service("[=domainServiceBean]")
@Validated
@Slf4j
@RequiredArgsConstructor
public class [=domainImplType] implements [=domainServiceType] {

    @Qualifier("[=repoBean]")
    private final [=repoType] repository;

    @Qualifier("[=persistenceConverterBean]")
    private final [=persistenceConverterType] converter;

    @Override
    @Transactional
    public [=domainType] create(@Valid [=domainType] value) {
        [=poType] po = converter.toSource(value);
        if (!repository.save(po)) {
            throw [=errorMapper].zeroRows("create", po.getId(), po.getVersion());
        }
        log.info("created {} id={}", "[=tableName]", po.getId());
        return converter.toTarget(po);
    }

    @Override
    @Transactional
    public [=domainType] update(@Valid [=domainType] value) {
        [=poType] current = repository.getById(value.getId());
        if (current == null) {
            throw [=errorMapper].missing(value.getId());
        }
        [=poType] po = converter.toSource(value);
        po.setId(current.getId());
        po.setTenantId(current.getTenantId());
        po.setCreateUserId(current.getCreateUserId());
        po.setCreateTime(current.getCreateTime());
        po.setVersion(value.getVersion());
        if (!repository.updateById(po)) {
            throw [=errorMapper].zeroRows("update", po.getId(), po.getVersion());
        }
        log.info("updated {} id={} version={}", "[=tableName]", po.getId(), po.getVersion());
        return converter.toTarget(po);
    }

    @Override
    @Transactional
    public void delete(Long id, Long expectedVersion) {
        [=poType] po = new [=poType]();
        po.setId(id);
        po.setVersion(expectedVersion);
        if (!repository.removeById(po)) {
            throw [=errorMapper].zeroRows("delete", id, expectedVersion);
        }
        log.info("deleted {} id={} version={}", "[=tableName]", id, expectedVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public [=domainType] detail(Long id) {
        [=poType] po = repository.getById(id);
        if (po == null) {
            throw [=errorMapper].missing(id);
        }
        return converter.toTarget(po);
    }

    @Override
    @Transactional(readOnly = true)
    public PageSlice<[=domainType]> query(@Valid [=domainQueryType] query) {
        int limit = query.getPage().pageSize();
        List<[=poType]> rows = new ArrayList<>(repository.getBaseMapper()
                .selectByQuery(query, limit + 1, query.getPage().offset()));
        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }
        return PageSlice.of(rows.stream().map(converter::toTarget).toList(), hasNext);
    }
}
