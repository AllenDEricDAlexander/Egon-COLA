package top.egon.cola.component.ruleengine.trace;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.tree.NodeType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RuleTraceRecorderTest {

    @Test
    void shouldRecordNodeTraceWhenEnabled() {
        RuleContext context = RuleContext.create("req-1", "trace-1");
        RuleTraceRecorder recorder = new RuleTraceRecorder(true);
        recorder.addNodeTrace(new NodeTrace("node-a", "Node A", NodeType.BIZ, 1, 1,
                Instant.now(), Instant.now(), 1L, "node-b", "next", RuleStatus.SUCCESS, null));

        RuleTrace trace = recorder.finish("rule-a", "Rule A", "CHAIN", context, RuleStatus.SUCCESS, null);

        assertThat(trace.ruleCode()).isEqualTo("rule-a");
        assertThat(trace.requestId()).isEqualTo("req-1");
        assertThat(trace.traceId()).isEqualTo("trace-1");
        assertThat(trace.nodeTraces()).hasSize(1);
    }
}
