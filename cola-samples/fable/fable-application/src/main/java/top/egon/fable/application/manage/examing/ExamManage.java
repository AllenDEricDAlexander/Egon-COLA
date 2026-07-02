package top.egon.fable.application.manage.examing;

import top.egon.fable.application.view.examing.ExamResultView;

public interface ExamManage {

    ExamResultView record(String courseId, String studentId, int score);

    ExamResultView getById(String examResultId);
}
