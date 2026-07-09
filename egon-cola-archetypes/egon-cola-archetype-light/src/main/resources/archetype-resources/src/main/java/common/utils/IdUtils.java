package ${package}.common.utils;

import java.util.UUID;

public final class IdUtils {
    private IdUtils() {
    }

    public static String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
