package top.egon.cola.component.dtp.executor.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @ClassName: ThreadPoolResizeSupportTest
 * @description: 线程池安全调整测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-29Day
 * @Version: 1.0
 */
public class ThreadPoolResizeSupportTest {

    @Test
    public void test_resizeLoweringMaxBelowCurrentCore() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10, 20,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );

        ThreadPoolResizeSupport.resize(executor, 5, 8);

        assertEquals(5, executor.getCorePoolSize());
        assertEquals(8, executor.getMaximumPoolSize());
    }

    @Test
    public void test_resizeRaisingCoreAboveCurrentMax() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );

        ThreadPoolResizeSupport.resize(executor, 12, 15);

        assertEquals(12, executor.getCorePoolSize());
        assertEquals(15, executor.getMaximumPoolSize());
    }

    @Test
    public void test_resizeRejectsInvalidSizes() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );

        IllegalArgumentException nonPositive = assertThrows(
                IllegalArgumentException.class,
                () -> ThreadPoolResizeSupport.resize(executor, 0, 10)
        );
        assertEquals("corePoolSize and maximumPoolSize must be positive", nonPositive.getMessage());

        IllegalArgumentException coreGreaterThanMax = assertThrows(
                IllegalArgumentException.class,
                () -> ThreadPoolResizeSupport.resize(executor, 11, 10)
        );
        assertEquals("corePoolSize must <= maximumPoolSize", coreGreaterThanMax.getMessage());
    }

}
