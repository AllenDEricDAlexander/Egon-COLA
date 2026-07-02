package top.egon.fable-web.common.utils;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {
    }

    public static String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
