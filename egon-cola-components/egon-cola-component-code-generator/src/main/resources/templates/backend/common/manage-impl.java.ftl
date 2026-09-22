package [=applicationPackage];

import [=domainFqn];
import [=domainQueryFqn];
import [=domainServiceFqn];
import [=applicationConverterPackage].[=createConverterType];
import [=applicationConverterPackage].[=updateConverterType];
import [=applicationConverterPackage].[=deleteConverterType];
import [=applicationConverterPackage].[=resultConverterType];
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageSlice;

@Service("[=manageBean]")
@Validated
@Slf4j
@RequiredArgsConstructor
public class [=manageImplType] implements [=manageType] {

    @Qualifier("[=domainServiceBean]")
    private final [=domainServiceType] domainService;

    @Qualifier("[=createConverterType?uncap_first]Impl")
    private final [=createConverterType] createConverter;

    @Qualifier("[=updateConverterType?uncap_first]Impl")
    private final [=updateConverterType] updateConverter;

    @Qualifier("[=deleteConverterType?uncap_first]Impl")
    private final [=deleteConverterType] deleteConverter;

    @Qualifier("[=resultConverterType?uncap_first]Impl")
    private final [=resultConverterType] resultConverter;

    @Override
    @Transactional
    public [=resultType] create(@Valid [=createCommandType] command) {
        log.info("create {}", "[=tableName]");
        return resultConverter.toTarget(domainService.create(createConverter.toTarget(command)));
    }

    @Override
    @Transactional
    public [=resultType] update(@Valid [=updateCommandType] command) {
        [=domainType] domain = updateConverter.toTarget(command);
        log.info("update {} id={} version={}", "[=tableName]", command.getId(), domain.getVersion());
        return resultConverter.toTarget(domainService.update(domain));
    }

    @Override
    @Transactional
    public void delete(@Valid [=deleteCommandType] command) {
        [=domainType] domain = deleteConverter.toTarget(command);
        log.info("delete {} id={} version={}", "[=tableName]", command.getId(), domain.getVersion());
        domainService.delete(domain.getId(), domain.getVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public [=resultType] detail(@Valid [=detailQueryType] query) {
        return resultConverter.toTarget(domainService.detail(query.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageSlice<[=resultType]> page(@Valid [=pageQueryType] query) {
        [=domainQueryType] domainQuery = new [=domainQueryType]();
        domainQuery.setPage(query.getPage());
[#list filterFields as field]
        domainQuery.set[=field.javaName?cap_first](query.get[=field.javaName?cap_first]());
[/#list]
        PageSlice<[=domainType]> slice = domainService.query(domainQuery);
        return PageSlice.of(slice.records().stream().map(resultConverter::toTarget).toList(), slice.hasNext());
    }
}
