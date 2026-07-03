package top.egon.fable.application.manage.examing;

import top.egon.fable.domain.entities.examing.ExamResult;

public interface ExamManage {

    ExamResult record(String courseId, String studentId, int score);

    ExamResult getById(String examResultId);
}
