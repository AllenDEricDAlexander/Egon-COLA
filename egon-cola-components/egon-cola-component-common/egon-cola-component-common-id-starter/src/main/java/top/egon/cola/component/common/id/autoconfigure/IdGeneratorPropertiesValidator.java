package top.egon.cola.component.common.id.autoconfigure;

import java.time.Duration;

final class IdGeneratorPropertiesValidator {

    private IdGeneratorPropertiesValidator() {
    }

    static void validate(IdGeneratorProperties properties) {
        Long machineId = properties.getMachineId();
        if (machineId == null) {
            throw new IllegalStateException(
                    "egon.cola.component.id.machine-id must be configured when enabled=true");
        }
        if (machineId < 0L || machineId > 1023L) {
            throw new IllegalStateException(
                    "egon.cola.component.id.machine-id must be between 0 and 1023: " + machineId);
        }

        Duration maxClockBackward = properties.getMaxClockBackward();
        if (maxClockBackward == null) {
            throw new IllegalStateException(
                    "egon.cola.component.id.max-clock-backward must not be null");
        }
        if (maxClockBackward.isNegative()) {
            throw new IllegalStateException(
                    "egon.cola.component.id.max-clock-backward must not be negative: " + maxClockBackward);
        }
    }
}
