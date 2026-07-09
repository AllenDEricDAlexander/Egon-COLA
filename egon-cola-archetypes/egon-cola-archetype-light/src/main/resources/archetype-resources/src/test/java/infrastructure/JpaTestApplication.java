package ${package}.infrastructure;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@Profile("jpa-test")
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = {
        "${package}.infrastructure.user.repo.jpa",
        "${package}.infrastructure.teaching.repo.jpa"
})
@EntityScan(basePackages = {
        "${package}.infrastructure.user.repo.po",
        "${package}.infrastructure.teaching.repo.po"
})
public class JpaTestApplication {
}
