package top.egon.cola.component.common.desensitize.strategy;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SensitiveStrategyRegistry {

    private final Map<SensitiveType, SensitiveStrategy> strategies;

    public SensitiveStrategyRegistry(Collection<? extends SensitiveStrategy> strategies) {
        EnumMap<SensitiveType, SensitiveStrategy> indexed = new EnumMap<>(SensitiveType.class);
        for (SensitiveStrategy strategy : strategies) {
            SensitiveStrategy previous = indexed.put(strategy.type(), strategy);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate sensitive strategy type: " + strategy.type()
                );
            }
        }
        this.strategies = Map.copyOf(indexed);
    }

    public static SensitiveStrategyRegistry defaults() {
        return new SensitiveStrategyRegistry(List.of(
                new MobileSensitiveStrategy(),
                new EmailSensitiveStrategy(),
                new IdCardSensitiveStrategy(),
                new BankCardSensitiveStrategy(),
                new NameSensitiveStrategy(),
                new AddressSensitiveStrategy(),
                new FullSensitiveStrategy()
        ));
    }

    public SensitiveStrategyRegistry withOverrides(
            Collection<? extends SensitiveStrategy> overrides) {
        EnumMap<SensitiveType, SensitiveStrategy> merged = new EnumMap<>(SensitiveType.class);
        merged.putAll(strategies);
        EnumMap<SensitiveType, SensitiveStrategy> uniqueOverrides =
                new EnumMap<>(SensitiveType.class);
        for (SensitiveStrategy strategy : overrides) {
            SensitiveStrategy previous = uniqueOverrides.put(strategy.type(), strategy);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate sensitive strategy override type: " + strategy.type()
                );
            }
        }
        merged.putAll(uniqueOverrides);
        return new SensitiveStrategyRegistry(merged.values());
    }

    public SensitiveStrategy get(SensitiveType type) {
        SensitiveStrategy strategy = strategies.get(Objects.requireNonNull(type, "type"));
        if (strategy == null) {
            throw new IllegalArgumentException("No sensitive strategy for type: " + type);
        }
        return strategy;
    }

    public String mask(SensitiveType type, String value) {
        return get(type).mask(value);
    }
}
