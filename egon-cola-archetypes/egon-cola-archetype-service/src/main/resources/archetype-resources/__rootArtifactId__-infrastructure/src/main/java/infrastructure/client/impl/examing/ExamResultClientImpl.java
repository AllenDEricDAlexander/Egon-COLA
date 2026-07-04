#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.impl.examing;

import java.util.Optional;

import ${package}.domain.client.examing.ExamResultClient;
import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.repos.examing.ExamResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component("examResultClientImpl")
@Validated
@RequiredArgsConstructor
public class ExamResultClientImpl implements ExamResultClient {

    @Qualifier("examResultRepositoryImpl")
    private final ExamResultRepository examResultRepository;

    @Override
    public ExamResult save(ExamResult examResult) {
        return examResultRepository.save(examResult);
    }

    @Override
    public Optional<ExamResult> findById(String examResultId) {
        return examResultRepository.findById(examResultId);
    }
}
