package top.egon.cola.component.tianshu.admin.model.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.Singular;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcPageResult<T> {

    @NotNull
    @Valid
    @Singular("record")
    private List<@Valid T> records = List.of();

    @Min(0)
    private long total;

    @Min(1)
    private int pageNo;

    @Min(1)
    private int pageSize;
}
