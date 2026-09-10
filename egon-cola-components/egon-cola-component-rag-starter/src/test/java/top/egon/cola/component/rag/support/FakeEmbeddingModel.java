package top.egon.cola.component.rag.support;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import top.egon.cola.component.rag.exception.RagEmbeddingException;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline {@link EmbeddingModel} with a configurable dimension count, deterministic vectors and a
 * failure hook. No provider, endpoint or key is involved.
 */
public class FakeEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    private final List<String> embeddedTexts = new ArrayList<>();

    private boolean failOnEmbed;

    public FakeEmbeddingModel(int dimensions) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        this.dimensions = dimensions;
    }

    public void failOnEmbed() {
        this.failOnEmbed = true;
    }

    public int invocationCount() {
        return embeddedTexts.size();
    }

    public List<String> embeddedTexts() {
        return List.copyOf(embeddedTexts);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>(instructions.size());
        for (int index = 0; index < instructions.size(); index++) {
            embeddings.add(new Embedding(vectorFor(instructions.get(index)), index));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        record(text);
        return vectorFor(text);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(embed(text));
        }
        return vectors;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private void record(String text) {
        if (failOnEmbed) {
            throw new RagEmbeddingException("simulated embedding failure");
        }
        embeddedTexts.add(text);
    }

    private float[] vectorFor(String text) {
        float[] vector = new float[dimensions];
        float seed = text == null ? 0f : text.hashCode() % 97;
        for (int index = 0; index < dimensions; index++) {
            vector[index] = seed + index;
        }
        return vector;
    }
}
