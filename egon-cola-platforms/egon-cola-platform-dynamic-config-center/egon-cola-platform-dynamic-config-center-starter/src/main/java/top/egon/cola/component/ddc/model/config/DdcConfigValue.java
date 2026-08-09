package top.egon.cola.component.ddc.model.config;

/**
 * 从 DDC Admin 读取的单个配置值。
 * / Single configuration value read from DDC Admin.
 */
public class DdcConfigValue {

    /**
     * 配置资源名。 / Configuration resource name.
     */
    private String resourceName;

    /**
     * 配置内容。 / Configuration content.
     */
    private String content;

    /**
     * 配置格式的线协议名称。 / Wire name of the configuration format.
     */
    private String format;

    /**
     * 配置版本。 / Configuration version.
     */
    private Long version;

    /**
     * 返回配置资源名。
     * / Returns the configuration resource name.
     *
     * @return 配置资源名 / configuration resource name
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * 设置配置资源名。
     * / Sets the configuration resource name.
     *
     * @param resourceName 配置资源名 / configuration resource name
     */
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * 返回配置内容。
     * / Returns the configuration content.
     *
     * @return 配置内容 / configuration content
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置配置内容。
     * / Sets the configuration content.
     *
     * @param content 配置内容 / configuration content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 返回配置格式名称。
     * / Returns the configuration format name.
     *
     * @return 配置格式名称 / configuration format name
     */
    public String getFormat() {
        return format;
    }

    /**
     * 设置配置格式名称。
     * / Sets the configuration format name.
     *
     * @param format 配置格式名称 / configuration format name
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * 返回配置版本。
     * / Returns the configuration version.
     *
     * @return 配置版本 / configuration version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * 设置配置版本。
     * / Sets the configuration version.
     *
     * @param version 配置版本 / configuration version
     */
    public void setVersion(Long version) {
        this.version = version;
    }
}
