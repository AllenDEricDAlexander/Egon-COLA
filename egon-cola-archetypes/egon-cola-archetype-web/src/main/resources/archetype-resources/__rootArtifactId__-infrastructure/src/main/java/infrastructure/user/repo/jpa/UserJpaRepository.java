package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.UserPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserPO, String> {

    boolean existsByEmail(String email);
}
