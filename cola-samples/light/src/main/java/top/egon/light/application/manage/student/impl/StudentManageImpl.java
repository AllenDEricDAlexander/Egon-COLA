package top.egon.light.application.manage.student.impl;

import top.egon.light.application.manage.student.StudentManage;
import top.egon.light.common.constants.ErrorCodes;
import top.egon.light.common.exceptions.BizException;
import top.egon.light.common.exceptions.NotFoundException;
import top.egon.light.common.utils.IdGenerator;
import top.egon.light.domain.common.Page;
import top.egon.light.domain.student.model.Student;
import top.egon.light.domain.student.repos.StudentRepository;
import top.egon.light.domain.student.service.StudentDomainService;
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

    @Override
    @Transactional(readOnly = true)
    public Page<Student> getPage(int currentPage, int pageSize) {
        return studentRepository.findPage(currentPage, pageSize);
    }
}
