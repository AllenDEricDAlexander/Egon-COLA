package top.egon.cola.component.yuheng.core.exchange;

/**
 * Protocol-independent body contract without fixing a buffered or streaming
 * transport representation.
 */
public interface GatewayBody {

    long contentLength();

    boolean replayable();
}
