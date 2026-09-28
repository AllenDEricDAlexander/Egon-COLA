package top.egon.cola.component.tianshu.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(value = "top.egon.cola.component.tianshu.admin.dao", lazyInitialization = "true")
public class DynamicConfigCenterAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicConfigCenterAdminApplication.class, args);
    }
}
