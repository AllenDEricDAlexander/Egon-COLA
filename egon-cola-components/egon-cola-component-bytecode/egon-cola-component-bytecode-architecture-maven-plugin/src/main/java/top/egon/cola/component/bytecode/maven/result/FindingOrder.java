package top.egon.cola.component.bytecode.maven.result;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;

import java.util.Comparator;

public final class FindingOrder {

    public static final Comparator<ArchitectureFinding> COMPARATOR = Comparator
            .comparing(ArchitectureFinding::ruleId, Comparator.nullsFirst(String::compareTo))
            .thenComparing(ArchitectureFinding::module, Comparator.nullsFirst(String::compareTo))
            .thenComparing(ArchitectureFinding::sourceClass, Comparator.nullsFirst(String::compareTo))
            .thenComparing(ArchitectureFinding::sourceMember, Comparator.nullsFirst(String::compareTo))
            .thenComparing(ArchitectureFinding::dependencyKind,
                    Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(ArchitectureFinding::targetClass, Comparator.nullsFirst(String::compareTo))
            .thenComparing(ArchitectureFinding::targetMember, Comparator.nullsFirst(String::compareTo));

    private FindingOrder() {
    }
}
