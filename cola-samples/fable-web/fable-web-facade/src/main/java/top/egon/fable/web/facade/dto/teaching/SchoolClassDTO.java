package top.egon.fable.web.facade.dto.teaching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClassDTO implements Serializable {
    private String id;
    private String name;
    private String gradeName;
    private List<String> userIds;
}
