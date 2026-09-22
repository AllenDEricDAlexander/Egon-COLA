package [=applicationPackage];

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageSlice;

@Validated
public interface [=manageType] {

    [=resultType] create(@Valid [=createCommandType] command);

    [=resultType] update(@Valid [=updateCommandType] command);

    void delete(@Valid [=deleteCommandType] command);

    [=resultType] detail(@Valid [=detailQueryType] query);

    PageSlice<[=resultType]> page(@Valid [=pageQueryType] query);
}
