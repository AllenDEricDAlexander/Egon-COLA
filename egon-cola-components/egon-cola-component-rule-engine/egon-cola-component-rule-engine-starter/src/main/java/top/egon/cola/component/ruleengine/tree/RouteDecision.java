package top.egon.cola.component.ruleengine.tree;

public final class RouteDecision {

    private final String targetCode;

    private final RuleNode<?, ?> targetNode;

    private final String reason;

    private final boolean end;

    private final boolean noRoute;

    private final Object endData;

    private RouteDecision(String targetCode, RuleNode<?, ?> targetNode, String reason,
                          boolean end, boolean noRoute, Object endData) {
        this.targetCode = targetCode;
        this.targetNode = targetNode;
        this.reason = reason;
        this.end = end;
        this.noRoute = noRoute;
        this.endData = endData;
    }

    public static RouteDecision toCode(String targetCode) {
        return new RouteDecision(targetCode, null, null, false, false, null);
    }

    public static RouteDecision toCode(String targetCode, String reason) {
        return new RouteDecision(targetCode, null, reason, false, false, null);
    }

    public static RouteDecision toNode(RuleNode<?, ?> targetNode, String reason) {
        return new RouteDecision(null, targetNode, reason, false, false, null);
    }

    public static RouteDecision end(Object data) {
        return new RouteDecision(null, null, null, true, false, data);
    }

    public static RouteDecision noRoute(String reason) {
        return new RouteDecision(null, null, reason, false, true, null);
    }

    public <R> R endData(Class<R> type) {
        return type.isInstance(endData) ? type.cast(endData) : null;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public RuleNode<?, ?> getTargetNode() {
        return targetNode;
    }

    public String getReason() {
        return reason;
    }

    public boolean isEnd() {
        return end;
    }

    public boolean isNoRoute() {
        return noRoute;
    }

    public String routeTo() {
        if (targetNode != null) {
            return targetNode.code();
        }
        return targetCode;
    }
}
