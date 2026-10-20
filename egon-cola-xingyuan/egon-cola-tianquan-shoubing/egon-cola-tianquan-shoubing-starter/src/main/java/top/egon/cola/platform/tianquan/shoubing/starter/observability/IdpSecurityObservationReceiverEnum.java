package top.egon.cola.platform.tianquan.shoubing.starter.observability;

/** Deployment-owned receiver identity; request data never becomes a metric dimension. */
public enum IdpSecurityObservationReceiverEnum {
    SHOUBING, JIANSHEN, TIANSHU, YUHENG_ADMIN, YAOGUANG_ADMIN, BIZ, MCP, LLM, EDGE, APPLICATION
}
