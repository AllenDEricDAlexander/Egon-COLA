package ${package}.adapter.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class RequestContextFilterTest {
    @Test
    void exposes_typed_context_and_always_clears_it() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestContextFilter.OPERATOR_ID_HEADER, "operator-1");
        request.addHeader(RequestContextFilter.REQUEST_ID_HEADER, "request-1");
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            assertThat(RequestContextHolder.getRequired().operatorId()).isEqualTo("operator-1");
            return null;
        }).when(chain).doFilter(any(), any());

        new RequestContextFilter().doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(RequestContextHolder.get()).isEmpty();
    }
}
