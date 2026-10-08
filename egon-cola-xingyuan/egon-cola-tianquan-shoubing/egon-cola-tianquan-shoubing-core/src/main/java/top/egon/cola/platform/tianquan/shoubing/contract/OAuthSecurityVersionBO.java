package top.egon.cola.platform.tianquan.shoubing.contract;

import jakarta.validation.constraints.PositiveOrZero;
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
public class OAuthSecurityVersionBO {

    @PositiveOrZero
    private long realm;

    @PositiveOrZero
    private long client;

    @PositiveOrZero
    private Long subject;
}
