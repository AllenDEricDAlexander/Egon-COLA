package top.egon.light.infrastructure.repo.student.jpa;

import top.egon.light.infrastructure.repo.student.po.StudentPo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentJpaRepository extends JpaRepository<StudentPo, String> {
    boolean existsByEmail(String email);
}
