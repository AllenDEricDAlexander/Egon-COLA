package top.egon.cola.component.ddc.redis;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRedisTopicSubscriptionTest {

    @Test
    void subscribesAndClosesEveryTopicExactlyOnce() {
        RTopic first = mock(RTopic.class);
        RTopic second = mock(RTopic.class);
        MessageListener<String> listener = mock(MessageListener.class);
        when(first.addListener(String.class, listener)).thenReturn(11);
        when(second.addListener(String.class, listener)).thenReturn(12);

        DdcRedisTopicSubscription<String> subscription =
                new DdcRedisTopicSubscription<>(
                        List.of(first, second),
                        String.class,
                        listener
                );

        assertThat(subscription.isActive()).isTrue();
        subscription.close();
        subscription.close();
        assertThat(subscription.isActive()).isFalse();
        verify(first).removeListener(11);
        verify(second).removeListener(12);
    }

    @Test
    void rollsBackEarlierRegistrationWhenLaterTopicFails() {
        RTopic first = mock(RTopic.class);
        RTopic second = mock(RTopic.class);
        MessageListener<String> listener = mock(MessageListener.class);
        when(first.addListener(String.class, listener)).thenReturn(11);
        doThrow(new IllegalStateException("subscribe failed"))
                .when(second).addListener(String.class, listener);

        assertThatThrownBy(() -> new DdcRedisTopicSubscription<>(
                List.of(first, second),
                String.class,
                listener
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("subscribe failed");
        verify(first).removeListener(11);
    }
}
