package ${package}.application.manage.student.impl;

import ${package}.application.manage.student.StudentManage;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.student.model.Student;
import ${package}.domain.student.repos.StudentRepository;
import ${package}.domain.student.service.StudentDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("studentManage")
@RequiredArgsConstructor
public class StudentManageImpl implements StudentManage {
    @Qualifier("studentRepositoryImpl")
    private final StudentRepository studentRepository;

    @Qualifier("studentDomainService")
    private final StudentDomainService studentDomainService;

    @Override
    @Transactional
    public Student register(String name, String email) {
        if (studentRepository.existsByEmail(email)) {
            throw new BizException(ErrorCodes.STUDENT_EMAIL_DUPLICATED, "student email already exists");
        }
        Student student = studentDomainService.register(IdGenerator.nextId(), name, email);
        try {
            return studentRepository.save(student);
        } catch (DataIntegrityViolationException exception) {
            throw new BizException(ErrorCodes.STUDENT_EMAIL_DUPLICATED, "student email already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Student getById(String studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.STUDENT_NOT_FOUND, "student not found"));
    }
}
