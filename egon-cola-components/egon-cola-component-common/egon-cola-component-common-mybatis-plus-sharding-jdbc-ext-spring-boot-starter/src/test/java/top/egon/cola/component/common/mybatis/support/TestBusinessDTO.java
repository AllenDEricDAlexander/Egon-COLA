package top.egon.cola.component.common.mybatis.support;

import jakarta.validation.constraints.NotBlank;

/**
 * Test-only transport object. Persistence and tenant fields are deliberately
 * absent from the Controller boundary.
 */
public final class TestBusinessDTO {

    @NotBlank
    private String title;
    private String payload;

    public TestBusinessDTO() {
    }

    public TestBusinessDTO(String title, String payload) {
        this.title = title;
        this.payload = payload;
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
}
