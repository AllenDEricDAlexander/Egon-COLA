package top.egon.cola.component.ddc.refresh;

import java.util.LinkedHashSet;
import java.util.Set;

public record DdcConfigurationChangedEvent(
        String resourceName,
        long version,
        String checksum,
        Set<String> changedKeys,
        Set<String> addedKeys,
        Set<String> updatedKeys,
        Set<String> removedKeys,
        Set<String> refreshedKeys,
        Set<String> restartRequiredKeys,
        String changeId
) {

    public DdcConfigurationChangedEvent {
        changedKeys = immutable(changedKeys);
        addedKeys = immutable(addedKeys);
        updatedKeys = immutable(updatedKeys);
        removedKeys = immutable(removedKeys);
        refreshedKeys = immutable(refreshedKeys);
        restartRequiredKeys = immutable(restartRequiredKeys);
    }

    private static Set<String> immutable(Set<String> values) {
        return Set.copyOf(new LinkedHashSet<>(values));
    }
}
