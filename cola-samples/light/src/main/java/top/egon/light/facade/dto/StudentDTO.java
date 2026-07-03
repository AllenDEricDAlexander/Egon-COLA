package top.egon.light.facade.dto;

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
public class StudentDTO implements Serializable {
    private String id;
    private String name;
    private String email;
    private String status;
    private List<String> courseIds;
}
