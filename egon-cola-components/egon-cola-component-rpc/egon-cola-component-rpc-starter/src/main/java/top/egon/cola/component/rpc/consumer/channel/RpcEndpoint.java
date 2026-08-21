package top.egon.cola.component.rpc.consumer.channel;

/**
 * 定义 RPC Consumer 建立传输 Channel 所需的中立端点信息。
 *
 * <p>Defines the registry-neutral endpoint data required to create an RPC
 * consumer transport channel.
 */
public interface RpcEndpoint {

    String host();

    int port();

    boolean secure();

    /** Relative selection weight; custom endpoints compiled before this method use 100. */
    default int weight() {
        return 100;
    }
}
