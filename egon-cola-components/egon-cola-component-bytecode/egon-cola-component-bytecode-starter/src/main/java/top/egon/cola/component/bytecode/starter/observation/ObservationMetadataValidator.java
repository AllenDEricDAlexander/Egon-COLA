package top.egon.cola.component.bytecode.starter.observation;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ObservationMetadataValidator {

    private static final int MAXIMUM_TAGS = 8;
    private static final int MAXIMUM_VALUE_LENGTH = 128;
    private static final Pattern TAG_KEY = Pattern.compile(
            "[a-zA-Z][a-zA-Z0-9_.-]{0,63}");
    private static final Set<String> RESERVED_KEYS = Set.of(
            "class", "method", "layer", "result", "exception_group",
            "virtual_thread", "trace_id", "request_id", "thread",
            "method_descriptor");

    public void validate(Map<String, String> staticTags) {
        if (staticTags.size() > MAXIMUM_TAGS) {
            throw new IllegalArgumentException(
                    "observation static tags must not exceed " + MAXIMUM_TAGS);
        }
        staticTags.forEach(this::validate);
    }

    private void validate(String key, String value) {
        if (key == null || !TAG_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("invalid observation static tag key");
        }
        if (RESERVED_KEYS.contains(key)) {
            throw new IllegalArgumentException("reserved observation static tag key: " + key);
        }
        if (value == null || value.length() > MAXIMUM_VALUE_LENGTH) {
            throw new IllegalArgumentException("invalid observation static tag value: " + key);
        }
        if (value.contains("${") || value.contains("#{")) {
            throw new IllegalArgumentException("dynamic observation static tag: " + key);
        }
    }
}
