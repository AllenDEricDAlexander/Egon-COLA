package top.egon.cola.component.gateway.contract.mcp.protocol;

public enum McpProtocolDialect {
    STABLE_2025_11_25("2025-11-25", false),
    RC_2026_07_28("2026-07-28", true),
    LEGACY_2024_SSE("legacy-2024-sse", false);

    private final String protocolVersion;
    private final boolean releaseCandidate;

    McpProtocolDialect(String protocolVersion, boolean releaseCandidate) {
        this.protocolVersion = protocolVersion;
        this.releaseCandidate = releaseCandidate;
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    public boolean releaseCandidate() {
        return releaseCandidate;
    }
}
