package [=applicationPackage];

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
public class [=resultType] {

    private String id;
[#list resultFields as field]

    private [=field.javaType] [=field.javaName];
[/#list]
}
