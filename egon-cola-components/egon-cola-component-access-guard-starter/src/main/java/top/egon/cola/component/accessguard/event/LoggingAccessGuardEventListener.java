package top.egon.cola.component.accessguard.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingAccessGuardEventListener implements AccessGuardEventListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingAccessGuardEventListener.class);

    @Override
    public void onEvent(AccessGuardEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("access guard event: {}", event);
        }
    }
}
