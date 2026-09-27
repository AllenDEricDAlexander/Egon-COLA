package top.egon.cola.component.tianshu.autoconfigure.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@ConfigurationProperties(prefix = "egon.cola.component.tianshu")
public class DdcAiResourceProperties {

    @NotNull
    @Valid
    @Builder.Default
    private DdcAiToggleDTO prompt = new DdcAiToggleDTO();

    @NotNull
    @Valid
    @Builder.Default
    private DdcAiToggleDTO skill = new DdcAiToggleDTO();
}
