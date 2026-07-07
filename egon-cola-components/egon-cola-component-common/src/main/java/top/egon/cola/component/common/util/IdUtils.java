package top.egon.cola.component.common.util;

import java.util.UUID;

/**
 * ID 工具，首批提供基于 JDK UUID 的轻量能力。
 */
public final class IdUtils {

    private IdUtils() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String simpleUuid() {
        return uuid().replace("-", "");
    }

    public static String shortUuid() {
        return simpleUuid().substring(0, 16);
    }
}
