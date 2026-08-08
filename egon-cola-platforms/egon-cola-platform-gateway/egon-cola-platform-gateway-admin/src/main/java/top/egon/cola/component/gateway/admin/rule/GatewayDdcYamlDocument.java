package top.egon.cola.component.gateway.admin.rule;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import top.egon.cola.component.ddc.format.DdcYamlConfigFormatStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GatewayDdcYamlDocument {

    public static final String ACTIVE_CONFIG_KEY = "gateway.rules.active";

    public static final String CHUNK_CONFIG_PREFIX = "gateway.rules.chunk.";

    private final Yaml parser;

    private final Yaml writer;

    private final DdcYamlConfigFormatStrategy validator =
            new DdcYamlConfigFormatStrategy();

    public GatewayDdcYamlDocument() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        this.parser = new Yaml(new SafeConstructor(loaderOptions));

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(
                DumperOptions.FlowStyle.BLOCK
        );
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setSplitLines(false);
        this.writer = new Yaml(dumperOptions);
    }

    public String putLeaf(String content, String propertyKey, String value) {
        validatePropertyKey(propertyKey);
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        Map<Object, Object> root = parse(content, true);
        List<String> segments = segments(propertyKey);
        List<LeafLocation> matches = findLeaves(root, segments, 0);
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "ambiguous YAML property path: " + propertyKey
            );
        }
        if (matches.size() == 1) {
            LeafLocation location = matches.getFirst();
            location.parent().put(location.key(), value);
        } else {
            container(root, segments).put(segments.getLast(), value);
        }
        return write(root);
    }

    public Removal removeLeaf(String content, String propertyKey) {
        validatePropertyKey(propertyKey);
        Map<Object, Object> root = parse(content, false);
        List<LeafLocation> matches = findLeaves(
                root,
                segments(propertyKey),
                0
        );
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "ambiguous YAML property path: " + propertyKey
            );
        }
        if (matches.isEmpty()) {
            return new Removal(content, false);
        }
        LeafLocation location = matches.getFirst();
        location.parent().remove(location.key());
        for (int index = location.ancestors().size() - 1;
                index >= 0;
                index--) {
            ParentLink ancestor = location.ancestors().get(index);
            if (!ancestor.child().isEmpty()) {
                break;
            }
            ancestor.parent().remove(ancestor.key());
        }
        return new Removal(write(root), true);
    }

    public Optional<String> leafValue(
            String content,
            String propertyKey) {
        validatePropertyKey(propertyKey);
        List<LeafLocation> matches = findLeaves(
                parse(content, false),
                segments(propertyKey),
                0
        );
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "ambiguous YAML property path: " + propertyKey
            );
        }
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        Object value = matches.getFirst().value();
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException(
                    "Gateway rule YAML leaf must be a string: "
                            + propertyKey
            );
        }
        return Optional.of(stringValue);
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> parse(String content, boolean allowEmpty) {
        if (content == null || content.isBlank()) {
            if (allowEmpty) {
                return new LinkedHashMap<>();
            }
            throw new IllegalArgumentException(
                    "application.yml must not be empty"
            );
        }
        Object loaded;
        try {
            loaded = parser.load(content);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "invalid application.yml",
                    exception
            );
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    "application.yml root must be a map"
            );
        }
        return (Map<Object, Object>) map;
    }

    private Map<Object, Object> container(
            Map<Object, Object> root,
            List<String> segments) {
        Map<Object, Object> current = root;
        int index = 0;
        while (index < segments.size() - 1) {
            PrefixMatch match = longestMapPrefix(current, segments, index);
            if (match != null) {
                current = match.map();
                index = match.nextIndex();
                continue;
            }
            String segment = segments.get(index);
            Object collision = matchingKey(current, segment);
            if (collision != null) {
                throw new IllegalArgumentException(
                        "YAML property path crosses a scalar: "
                                + String.join(".", segments)
                );
            }
            Map<Object, Object> child = new LinkedHashMap<>();
            current.put(segment, child);
            current = child;
            index++;
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private PrefixMatch longestMapPrefix(
            Map<Object, Object> current,
            List<String> segments,
            int index) {
        PrefixMatch selected = null;
        for (Map.Entry<Object, Object> entry : current.entrySet()) {
            List<String> keySegments = segments(String.valueOf(entry.getKey()));
            if (!matches(segments, index, keySegments)
                    || index + keySegments.size() >= segments.size()) {
                continue;
            }
            if (!(entry.getValue() instanceof Map<?, ?> child)) {
                throw new IllegalArgumentException(
                        "YAML property path crosses a scalar: "
                                + String.join(".", segments)
                );
            }
            PrefixMatch candidate = new PrefixMatch(
                    (Map<Object, Object>) child,
                    index + keySegments.size()
            );
            if (selected == null
                    || candidate.nextIndex() > selected.nextIndex()) {
                selected = candidate;
            }
        }
        return selected;
    }

    private List<LeafLocation> findLeaves(
            Map<Object, Object> current,
            List<String> segments,
            int index) {
        return findLeaves(current, segments, index, List.of());
    }

    @SuppressWarnings("unchecked")
    private List<LeafLocation> findLeaves(
            Map<Object, Object> current,
            List<String> segments,
            int index,
            List<ParentLink> ancestors) {
        List<LeafLocation> matches = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : current.entrySet()) {
            List<String> keySegments = segments(String.valueOf(entry.getKey()));
            if (!matches(segments, index, keySegments)) {
                continue;
            }
            int nextIndex = index + keySegments.size();
            if (nextIndex == segments.size()) {
                matches.add(new LeafLocation(
                        current,
                        entry.getKey(),
                        entry.getValue(),
                        ancestors
                ));
            } else if (entry.getValue() instanceof Map<?, ?> child) {
                List<ParentLink> childAncestors =
                        new ArrayList<>(ancestors);
                childAncestors.add(new ParentLink(
                        current,
                        entry.getKey(),
                        (Map<Object, Object>) child
                ));
                matches.addAll(findLeaves(
                        (Map<Object, Object>) child,
                        segments,
                        nextIndex,
                        List.copyOf(childAncestors)
                ));
            }
        }
        return matches;
    }

    private boolean matches(
            List<String> propertySegments,
            int propertyIndex,
            List<String> keySegments) {
        if (propertyIndex + keySegments.size()
                > propertySegments.size()) {
            return false;
        }
        for (int index = 0; index < keySegments.size(); index++) {
            if (!propertySegments.get(propertyIndex + index)
                    .equals(keySegments.get(index))) {
                return false;
            }
        }
        return true;
    }

    private Object matchingKey(Map<Object, Object> map, String segment) {
        return map.keySet().stream()
                .filter(key -> segment.equals(String.valueOf(key)))
                .findFirst()
                .orElse(null);
    }

    private List<String> segments(String propertyKey) {
        return List.of(propertyKey.split("\\.", -1));
    }

    private String write(Map<Object, Object> root) {
        String content = writer.dump(root);
        try {
            validator.load(
                    DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME,
                    content,
                    1L
            );
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Gateway produced an invalid application.yml",
                    exception
            );
        }
        return content;
    }

    private void validatePropertyKey(String propertyKey) {
        if (propertyKey == null
                || propertyKey.isBlank()
                || propertyKey.endsWith(".")
                || propertyKey.contains("..")
                || !(propertyKey.equals(ACTIVE_CONFIG_KEY)
                || propertyKey.startsWith(CHUNK_CONFIG_PREFIX))) {
            throw new IllegalArgumentException(
                    "unsupported Gateway rule property: " + propertyKey
            );
        }
    }

    public record Removal(String content, boolean removed) {
    }

    private record LeafLocation(
            Map<Object, Object> parent,
            Object key,
            Object value,
            List<ParentLink> ancestors) {
    }

    private record ParentLink(
            Map<Object, Object> parent,
            Object key,
            Map<Object, Object> child) {
    }

    private record PrefixMatch(
            Map<Object, Object> map,
            int nextIndex) {
    }
}
