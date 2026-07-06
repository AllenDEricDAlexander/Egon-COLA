package top.egon.cola.component.dtp.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @ClassName: Application
 * @description: Application
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-13Day-上午 8:41
 * @Version: 1.0
 */
@Configuration
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {"top.egon.cola.component.dtp.test", "top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool"})
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
