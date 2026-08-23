#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.migration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManualSchemaIntegrationTest {

    @Test
    void shouldDescribeTheCompleteEvaluationPhysicalTopology() {
        String master = ManualSchemaTestSupport.read(
                "db/manual/postgresql/master-data/001__create_evaluation_master_data_schema.sql");
        String shard = ManualSchemaTestSupport.read(
                "db/manual/postgresql/shard/002__create_evaluation_sharded_schema.sql");

        assertThat(master).contains("CREATE TABLE course", "id BIGINT");
        assertThat(shard).contains(
                "course_schedule_0", "course_schedule_1",
                "exam_0", "exam_1",
                "exam_paper_0", "exam_paper_1",
                "score_0", "score_1");
        assertThat(shard).contains(
                "uk_exam_paper_0_exam", "uk_exam_paper_1_exam",
                "uk_score_0_exam_student", "uk_score_1_exam_student");
        assertThat(shard).contains("BIGINT").contains("FOREIGN KEY (exam_id)");
    }
}
