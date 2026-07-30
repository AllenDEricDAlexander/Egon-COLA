package top.egon.cola.component.gateway.core.provider;

public interface ProviderServiceRegistry {

    ProviderCatalogSnapshot getServiceKeys(ProviderQuery query);

    ProviderServiceSnapshot getInstances(ProviderServiceKey key);

    ProviderSubscription subscribeServices(
            ProviderQuery query,
            ProviderCatalogListener listener
    );

    ProviderSubscription subscribe(
            ProviderServiceKey key,
            ProviderSnapshotListener listener
    );

    @FunctionalInterface
    interface ProviderCatalogListener {

        void onSnapshot(ProviderCatalogSnapshot snapshot);
    }

    @FunctionalInterface
    interface ProviderSnapshotListener {

        void onSnapshot(ProviderServiceSnapshot snapshot);
    }
}
