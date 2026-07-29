package top.egon.cola.component.accessguard.key;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSha256KeyHasherTest {

    @Test
    void hashesNormalizedUtf8ContentWithConfiguredSecret() {
        assertThat(new HmacSha256KeyHasher().hash(
                "tenant=tenant-1|user=user-1",
                "test-secret"))
                .isEqualTo("66d21ce28f79756142819d50d571463222b5fd0950d226d873db3a75494b482d");
    }
}
