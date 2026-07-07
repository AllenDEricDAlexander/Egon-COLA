package top.egon.cola.component.common.model;

import java.io.Serial;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * common 基础模型，提供序列化和扩展字段能力。
 */
public class BaseModel implements Serializable {


    @Serial
    private static final long serialVersionUID = 1L;

    private Map<String, Object> extensions = new LinkedHashMap<>();

    public Object getExtension(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return extensions.get(key);
    }

    public void putExtension(String key, Object value) {
        if (key == null || key.isBlank()) {
            return;
        }
        extensions.put(key, value);
    }

    public Map<String, Object> getExtensions() {
        return Collections.unmodifiableMap(extensions);
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extensions);
    }
}
