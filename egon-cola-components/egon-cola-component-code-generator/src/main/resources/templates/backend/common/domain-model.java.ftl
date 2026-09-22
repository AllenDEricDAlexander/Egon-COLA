package [=domainPackage];

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
public class [=domainType] {

    private Long id;

    private Long version;
[#list domainFields as field]

    private [=field.javaType] [=field.javaName];
[/#list]
}
