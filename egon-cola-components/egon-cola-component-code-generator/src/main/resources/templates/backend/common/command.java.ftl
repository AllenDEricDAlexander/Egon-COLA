package [=applicationPackage];

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
[#if commandKind == "create"]
public class [=createCommandType] {
[#list createFields as field]
[#if field.length?has_content]
    @Size(max = [=field.length])
[/#if]
[#if field.nullable == "false"]
    @NotNull
[/#if]
    private [=field.javaType] [=field.javaName];
[/#list]
}
[#elseif commandKind == "update"]
public class [=updateCommandType] {

    @NotNull
    @Positive
    private Long id;

    @NotNull
    @Min(0)
    private Long expectedVersion;
[#list updateFields as field]
[#if field.length?has_content]
    @Size(max = [=field.length])
[/#if]
[#if field.nullable == "false"]
    @NotNull
[/#if]
    private [=field.javaType] [=field.javaName];
[/#list]
}
[#else]
public class [=deleteCommandType] {

    @NotNull
    @Positive
    private Long id;

    @NotNull
    @Min(0)
    private Long expectedVersion;
}
[/#if]
