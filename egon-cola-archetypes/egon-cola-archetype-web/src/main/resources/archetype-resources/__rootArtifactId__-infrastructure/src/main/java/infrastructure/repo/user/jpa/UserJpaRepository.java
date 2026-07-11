package ${package}.infrastructure.repo.user.jpa;

import ${package}.infrastructure.repo.user.po.UserPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserPO, String> {

    boolean existsByEmail(String email);
}
