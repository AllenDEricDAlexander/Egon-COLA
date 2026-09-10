/**
 * RAG engine mechanics: document extraction, chunking, embedding, retrieval and document
 * storage extension points.
 *
 * <p>The component owns no provider configuration, no API key, no schema and no HTTP surface.
 * The host application provides a named {@code EmbeddingModel} and a named {@code VectorStore};
 * this component only resolves them by name and drives them.
 */
package top.egon.cola.component.rag;
