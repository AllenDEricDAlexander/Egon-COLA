package [=controllerPackage];

import [=applicationPackage].[=manageType];
import [=applicationPackage].[=createCommandType];
import [=applicationPackage].[=updateCommandType];
import [=applicationPackage].[=deleteCommandType];
import [=applicationPackage].[=detailQueryType];
import [=applicationPackage].[=pageQueryType];
import [=applicationPackage].[=resultType];
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageSlice;

@RestController("[=controllerBean]")
@RequestMapping("[=apiBasePath]")
@RequiredArgsConstructor
@Slf4j
public class [=controllerType] {

    @Qualifier("[=manageBean]")
    private final [=manageType] manage;

    @PostMapping
    public [=resultType] create(@Valid @RequestBody [=createCommandType] command) {
        log.info("http create {}", "[=tableName]");
        return manage.create(command);
    }

    @GetMapping("/{id}")
    public [=resultType] detail(@PathVariable String id) {
        return manage.detail(new [=detailQueryType]().setId(parseId(id)));
    }

    @GetMapping
    public PageSlice<[=resultType]> page(@Valid [=pageQueryType] query) {
        return manage.page(query);
    }

    @PutMapping("/{id}")
    public [=resultType] update(@PathVariable String id, @Valid @RequestBody [=updateCommandType] command) {
        command.setId(parseId(id));
        return manage.update(command);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id, @RequestParam("expectedVersion") long expectedVersion) {
        manage.delete(new [=deleteCommandType]().setId(parseId(id)).setExpectedVersion(expectedVersion));
    }

    private static long parseId(String id) {
        if (id == null || !id.matches("[0-9]+")) {
            throw new IllegalArgumentException("id must be a positive decimal string");
        }
        return Long.parseLong(id);
    }
}
