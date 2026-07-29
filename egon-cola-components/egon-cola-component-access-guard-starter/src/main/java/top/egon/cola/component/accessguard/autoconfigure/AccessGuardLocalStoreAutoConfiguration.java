package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Fallback;
import top.egon.cola.component.accessguard.core.plan.GuardPlanProperties;
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
    @Fallback
    @ConditionalOnMissingBean(name = "accessGuardLocalDenyListStore")
    public LocalDenyListStore accessGuardLocalDenyListStore(Clock accessGuardClock, GuardPlanProperties properties) {
        return new LocalDenyListStore(accessGuardClock, properties.getLocal().getMaxEntries());
    }

    @Bean
    @Fallback
    @ConditionalOnMissingBean(name = "accessGuardLocalAllowListStore")
    public LocalAllowListStore accessGuardLocalAllowListStore(Clock accessGuardClock, GuardPlanProperties properties) {
        return new LocalAllowListStore(accessGuardClock, properties.getLocal().getMaxEntries());
    }

    @Bean
    @Fallback
    @ConditionalOnMissingBean(name = "accessGuardLocalPenaltyStore")
    public LocalPenaltyStore accessGuardLocalPenaltyStore(Clock accessGuardClock, GuardPlanProperties properties) {
        return new LocalPenaltyStore(accessGuardClock, properties.getLocal().getMaxEntries());
    }

    @Bean
    @Fallback
    @ConditionalOnMissingBean(name = "accessGuardLocalRateLimitBackend")
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
            LocalPenaltyStore penaltyStore,
            LocalRateLimitBackend rateLimitBackend
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
