package top.egon.cola.component.agentflow.runtime;

import io.reactivex.rxjava3.core.Completable;
import top.egon.cola.component.agentflow.api.AgentFlowDescriptorDTO;

import java.util.List;

/** Read-only runtime registry with an explicit closing lifecycle. */
public interface AgentFlowRegistry {

    List<AgentFlowDescriptorDTO> listFlows();

    AgentFlowRuntimeBO require(String flowId);

    boolean beginClosing();

    Completable close();

    boolean isClosed();
}
