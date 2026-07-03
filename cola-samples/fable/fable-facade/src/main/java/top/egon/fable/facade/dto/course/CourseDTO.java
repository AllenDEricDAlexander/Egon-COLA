package top.egon.fable.facade.dto.course;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO implements Serializable {

    private String id;

    private String name;

    private int credit;

    private String status;
}
