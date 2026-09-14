package top.egon.cola.component.common.mybatis.support;

import jakarta.validation.constraints.NotBlank;

/**
 * Test-only business Service process object.
 */
public final class TestBusinessPO {

    @NotBlank
    private String title;
    private String payload;
    private String requestedState;

    public TestBusinessPO() {
    }

    public TestBusinessPO(String title, String payload, String requestedState) {
        this.title = title;
        this.payload = payload;
        this.requestedState = requestedState;
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

    public String getRequestedState() {
        return requestedState;
    }

    public void setRequestedState(String requestedState) {
        this.requestedState = requestedState;
    }
}
