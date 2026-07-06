package top.egon.cola.component.dtp.test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @ClassName: Main
 * @description: 日常测试使用
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午3:45
 * @Version: 1.0
 */
public class Main {
    public static void main(String[] args) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println(dateTimeFormatter.format(LocalDateTime.now()));
    }
}
