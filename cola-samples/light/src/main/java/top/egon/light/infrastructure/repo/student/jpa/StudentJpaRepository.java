package top.egon.light.infrastructure.repo.student.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.light.infrastructure.repo.student.po.StudentPo;

public interface StudentJpaRepository extends JpaRepository<StudentPo, String> {
    boolean existsByEmail(String email);
}
