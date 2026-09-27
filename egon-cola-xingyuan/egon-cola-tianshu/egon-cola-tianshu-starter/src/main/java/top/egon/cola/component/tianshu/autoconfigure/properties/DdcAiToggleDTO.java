package top.egon.cola.component.tianshu.autoconfigure.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiToggleDTO {

    @Builder.Default
    private boolean enabled = false;
}
