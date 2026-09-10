package top.egon.cola.component.rag.support;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link VectorStore} that records every call and can be told to fail at a chosen point.
 *
 * <p>Retrieval returns the stored documents whose metadata satisfies the request's filter
 * expression, truncated to {@code topK}; this is enough to assert forced filtering without a
 * database.
 */
public class FakeVectorStore implements VectorStore {

    /** Where the next call should fail. */
    public enum FailurePoint {
        NONE, ADD, DELETE, SEARCH
    }

    private final Map<String, Document> documents = new LinkedHashMap<>();

    private final List<Filter.Expression> deletedExpressions = new ArrayList<>();

    private final List<SearchRequest> searchRequests = new ArrayList<>();

    private FailurePoint failurePoint = FailurePoint.NONE;

    public void failOn(FailurePoint point) {
        this.failurePoint = point;
    }

    public void reset() {
        documents.clear();
        deletedExpressions.clear();
        searchRequests.clear();
        failurePoint = FailurePoint.NONE;
    }

    public List<Document> addedDocuments() {
        return List.copyOf(documents.values());
    }

    public List<Filter.Expression> deletedExpressions() {
        return List.copyOf(deletedExpressions);
    }

    public List<SearchRequest> searchRequests() {
        return List.copyOf(searchRequests);
    }

    @Override
    public void add(List<Document> toAdd) {
        if (failurePoint == FailurePoint.ADD) {
            throw new IllegalStateException("simulated add failure");
        }
        for (Document document : toAdd) {
            documents.put(document.getId(), document);
        }
    }

    @Override
    public void delete(List<String> ids) {
        for (String id : ids) {
            documents.remove(id);
        }
    }

    @Override
    public void delete(Filter.Expression expression) {
        if (failurePoint == FailurePoint.DELETE) {
            throw new IllegalStateException("simulated delete failure");
        }
        deletedExpressions.add(expression);
        documents.values().removeIf(document -> matches(expression, document));
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        if (failurePoint == FailurePoint.SEARCH) {
            throw new IllegalStateException("simulated search failure");
        }
        searchRequests.add(request);
        return documents.values().stream()
                .filter(document -> request.getFilterExpression() == null
                        || matches(request.getFilterExpression(), document))
                .limit(request.getTopK())
                .toList();
    }

    private static boolean matches(Filter.Expression expression, Document document) {
        if (expression == null) {
            return true;
        }
        return switch (expression.type()) {
            case AND -> matches(asExpression(expression.left()), document)
                    && matches(asExpression(expression.right()), document);
            case EQ -> {
                // EQ is (key, value); anything else cannot be evaluated by this double.
                if (!(expression.left() instanceof Filter.Key key)
                        || !(expression.right() instanceof Filter.Value value)) {
                    yield true;
                }
                Object actual = document.getMetadata().get(key.key());
                yield actual != null && String.valueOf(actual).equals(String.valueOf(value.value()));
            }
            default -> true;
        };
    }

    private static Filter.Expression asExpression(Filter.Operand operand) {
        return operand instanceof Filter.Expression expression ? expression : null;
    }
}
