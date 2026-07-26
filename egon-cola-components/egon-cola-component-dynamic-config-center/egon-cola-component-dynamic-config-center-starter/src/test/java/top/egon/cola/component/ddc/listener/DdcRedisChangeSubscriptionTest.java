package top.egon.cola.component.ddc.listener;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRedisChangeSubscriptionTest {

    @Test
    void subscribesAndClosesBothV2AndLegacyTopics() {
        RTopic v2 = mock(RTopic.class);
        RTopic legacy = mock(RTopic.class);
        DdcRedisChangeListener listener = mock(DdcRedisChangeListener.class);
        when(v2.addListener(DdcPublishMessage.class, listener)).thenReturn(11);
        when(legacy.addListener(DdcPublishMessage.class, listener)).thenReturn(12);

        DdcRedisChangeSubscription subscription =
                new DdcRedisChangeSubscription(List.of(v2, legacy), listener);

        assertThat(subscription.isActive()).isTrue();
        subscription.close();
        assertThat(subscription.isActive()).isFalse();
        verify(v2).removeListener(11);
        verify(legacy).removeListener(12);
    }
}
