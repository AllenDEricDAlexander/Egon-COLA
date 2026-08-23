package ${package}.domain.teaching.repos;

import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.vos.SchoolClassId;

import java.util.Optional;

public interface SchoolClassRepository {
    SchoolClass save(SchoolClass schoolClass);

    Optional<SchoolClassAggregate> findAggregateById(SchoolClassId schoolClassId);

    void saveAggregate(SchoolClassAggregate aggregate);
}
