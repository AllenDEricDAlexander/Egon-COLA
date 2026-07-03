package top.egon.fable.facade.dto.examing;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultDTO implements Serializable {

    private String id;

    private String courseId;

    private String studentId;

    private int score;

    private String status;
}
