package ${package}.application.manage.student.impl;

import ${package}.application.manage.student.StudentManage;
import ${package}.application.manage.student.StudentView;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.student.model.Student;
import ${package}.domain.student.repos.StudentRepository;
import ${package}.domain.student.service.StudentDomainService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentManageImpl implements StudentManage {
    private final StudentRepository studentRepository;
    private final StudentDomainService studentDomainService;

    public StudentManageImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
        this.studentDomainService = new StudentDomainService();
    }

    @Override
    @Transactional
    public StudentView register(String name, String email) {
        if (studentRepository.existsByEmail(email)) {
            throw new BizException(ErrorCodes.STUDENT_EMAIL_DUPLICATED, "student email already exists");
        }
        Student student = studentDomainService.register(IdGenerator.nextId(), name, email);
        try {
            return toView(studentRepository.save(student));
        } catch (DataIntegrityViolationException exception) {
            throw new BizException(ErrorCodes.STUDENT_EMAIL_DUPLICATED, "student email already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StudentView getById(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.STUDENT_NOT_FOUND, "student not found"));
        return toView(student);
    }

    private StudentView toView(Student student) {
        return new StudentView(student.getId(), student.getName(), student.getEmail(), student.getStatus().name(), student.getCourseIds());
    }
}
