package top.egon.cola.component.common.mybatis.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/**
 * Test-only business Model fixture.
 */
@TableName("test_business_record")
public class TestBusinessModel extends EgonModel<TestBusinessModel> {

    @NotBlank
    private String title;
    private String payload;

    @Version
    private Long version;

    public TestBusinessModel businessValues(String title, String payload) {
        this.title = title;
        this.payload = payload;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
