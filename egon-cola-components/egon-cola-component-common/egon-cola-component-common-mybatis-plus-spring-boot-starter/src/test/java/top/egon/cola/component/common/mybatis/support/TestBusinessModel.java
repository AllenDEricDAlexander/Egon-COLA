package top.egon.cola.component.common.mybatis.support;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/**
 * Test-only business Model fixture.
 */
@TableName("test_business_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class TestBusinessModel extends EgonModel<TestBusinessModel> {

    @NotBlank
    @TableField("title")
    private String title;

    @TableField("payload")
    private String payload;

    @Version
    @TableField("version")
    private Long version;

    public TestBusinessModel businessValues(String title, String payload) {
        this.title = title;
        this.payload = payload;
        return this;
    }

}
