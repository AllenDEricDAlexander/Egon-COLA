package top.egon.cola.archetype.source.agent.infrastructure.knowledge;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class RepositoryPersistenceContractTest {
    @Test
    void queriesAreBoundXmlAndTransitionsRetainStatusAndVersion() throws Exception {
        for (String name : java.util.List.of("KnowledgeBase", "KnowledgeDocument")) {
            String xml = Files.readString(Path.of("src/main/resources/mybatis/mapper/knowledge/" + name + "DAO.xml"));
            assertThat(xml).contains("deleted_at IS NULL", "MP_OPTLOCK_VERSION_ORIGINAL", "LIMIT #{size} OFFSET #{offset}", "selectActiveByIds").doesNotContain("${", "is_deleted");
        }
        String document = Files.readString(Path.of("src/main/resources/mybatis/mapper/knowledge/KnowledgeDocumentDAO.xml"));
        assertThat(document).contains("expectedStatuses", "version = version + 1", "knowledge_base_id = #{baseId}");
    }
}
