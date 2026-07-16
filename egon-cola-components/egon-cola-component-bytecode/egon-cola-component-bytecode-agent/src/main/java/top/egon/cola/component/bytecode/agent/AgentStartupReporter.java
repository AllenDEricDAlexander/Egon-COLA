package top.egon.cola.component.bytecode.agent;

import top.egon.cola.component.bytecode.bridge.BridgeProtocol;

public final class AgentStartupReporter {

    public String startupSummary(
            AgentConfiguration configuration,
            String state,
            boolean bridgeRegistered
    ) {
        return "Egon bytecode Agent"
                + " state=" + state
                + " protocol=" + BridgeProtocol.MAJOR + "." + BridgeProtocol.MINOR
                + " java=" + Runtime.version().feature()
                + " requestedFeatures=" + configuration.features()
                + " effectiveFeatures=" + configuration.features()
                + " includeCount=" + configuration.includes().size()
                + " includeDigest=" + configuration.includeDigest()
                + " excludeCount=" + configuration.excludes().size()
                + " excludeDigest=" + configuration.excludeDigest()
                + " failurePolicy=" + configuration.failurePolicy()
                + " bridgeRegistered=" + bridgeRegistered;
    }

    public String failureSummary(Throwable failure) {
        String message = failure == null || failure.getMessage() == null
                ? "" : failure.getMessage().replace('\r', ' ').replace('\n', ' ');
        if (message.length() > 256) {
            message = message.substring(0, 256);
        }
        return "Egon bytecode Agent state=FAILED cause="
                + (failure == null ? "" : failure.getClass().getName())
                + " message=" + message;
    }
}
