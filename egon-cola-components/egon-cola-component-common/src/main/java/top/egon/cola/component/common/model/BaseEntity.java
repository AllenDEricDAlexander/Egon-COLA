package top.egon.cola.component.common.model;

/**
 * 可选实体基础类，提供通用 id 字段。
 */
public class BaseEntity<ID> extends BaseModel {

    private static final long serialVersionUID = 1L;

    private ID id;

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }
}
