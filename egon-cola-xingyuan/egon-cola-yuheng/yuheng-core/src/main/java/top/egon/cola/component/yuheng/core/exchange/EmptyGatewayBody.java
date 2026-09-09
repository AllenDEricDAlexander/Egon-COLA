package top.egon.cola.component.yuheng.core.exchange;

public enum EmptyGatewayBody implements GatewayBody {

    INSTANCE;

    @Override
    public long contentLength() {
        return 0;
    }

    @Override
    public boolean replayable() {
        return true;
    }
}
