package [=domainPackage];

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.core.pojo.PageQuery;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class [=domainQueryType] {
[#list filterFields as field]

    private [=field.javaType] [=field.javaName];
[/#list]

    @Valid
    @NotNull
    @Builder.Default
    private PageQuery page = PageQuery.defaultPage();
}
