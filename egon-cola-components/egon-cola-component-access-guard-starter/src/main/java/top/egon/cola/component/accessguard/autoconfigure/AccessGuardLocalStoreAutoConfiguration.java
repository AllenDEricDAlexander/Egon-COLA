package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.core.plan.GuardPlanProperties;
import top.egon.cola.component.accessguard.store.AllowListStore;
import top.egon.cola.component.accessguard.store.DenyListStore;
import top.egon.cola.component.accessguard.store.PenaltyStore;
import top.egon.cola.component.accessguard.store.RateLimitBackend;
import top.egon.cola.component.accessguard.store.local.LocalAllowListStore;
import top.egon.cola.component.accessguard.store.local.LocalDenyListStore;
import top.egon.cola.component.accessguard.store.local.LocalPenaltyStore;
import top.egon.cola.component.accessguard.store.local.LocalRateLimitBackend;
import top.egon.cola.component.accessguard.store.local.LocalStateCleaner;

import java.time.Clock;
import java.util.List;

@AutoConfiguration
@ConditionalOnProperty(
        prefix = GuardPlanProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AccessGuardLocalStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock accessGuardClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(DenyListStore.class)
    public LocalDenyListStore accessGuardLocalDenyListStore(Clock accessGuardClock, GuardPlanProperties properties) {
        return new LocalDenyListStore(accessGuardClock, properties.getLocal().getMaxEntries());
    }

    @Bean
    @ConditionalOnMissingBean(AllowListStore.class)
    public LocalAllowListStore accessGuardLocalAllowListStore(Clock accessGuardClock, GuardPlanProperties properties) {
        return new LocalAllowListStore(accessGuardClock, properties.getLocal().getMaxEntries());
    }

    @Bean
    @ConditionalOnMissingBean(PenaltyStore.class)
    public LocalPenaltyStore accessGuardLocalPenaltyStore(Clock accessGuardClock, GuardPlanProperties properties) {
        return new LocalPenaltyStore(accessGuardClock, properties.getLocal().getMaxEntries());
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitBackend.class)
    public LocalRateLimitBackend accessGuardLocalRateLimitBackend(GuardPlanProperties properties) {
        return new LocalRateLimitBackend(
                System::nanoTime,
                properties.getLocal().getMaxEntries(),
                properties.getLocal().getIdleTtl());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public LocalStateCleaner accessGuardLocalStateCleaner(
            GuardPlanProperties properties,
            PenaltyStore penaltyStore,
            RateLimitBackend rateLimitBackend
    ) {
        List<Runnable> actions = new java.util.ArrayList<>();
        actions.add(penaltyStore::evictExpired);
        actions.add(rateLimitBackend::evictExpired);
        return new LocalStateCleaner(
                "access-guard-local-cleaner",
                properties.getLocal().getCleanupInterval(),
                actions);
    }
}
