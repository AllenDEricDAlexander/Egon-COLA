package top.egon.cola.platform.rbac3.admin.application.port;

import java.time.Instant;

public interface DatabaseClock {

    Instant transactionNow();
}
