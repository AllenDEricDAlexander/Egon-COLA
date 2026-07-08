package top.egon.cola.component.ruleengine.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.chain.AbstractSingletonRuleLink;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineLoginSingletonChainSampleTest {

    @Test
    void shouldRunLoginSingletonChain() {
        AccountCheck accountCheck = new AccountCheck();
        PasswordCheck passwordCheck = new PasswordCheck();
        StatusCheck statusCheck = new StatusCheck();
        accountCheck.appendNext(passwordCheck).appendNext(statusCheck);

        RuleResult<String> result = accountCheck.handle(new LoginRequest("egon", "secret", true),
                RuleContext.create());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("login-allowed");
    }

    private static final class AccountCheck extends AbstractSingletonRuleLink<LoginRequest, String> {

        @Override
        protected RuleResult<String> apply(LoginRequest request, RuleContext context) {
            return request.account() == null || request.account().isBlank()
                    ? RuleResult.stop(600301, "account required", "login-blocked")
                    : RuleResult.success(null);
        }
    }

    private static final class PasswordCheck extends AbstractSingletonRuleLink<LoginRequest, String> {

        @Override
        protected RuleResult<String> apply(LoginRequest request, RuleContext context) {
            return request.password() == null || request.password().isBlank()
                    ? RuleResult.stop(600302, "password required", "login-blocked")
                    : RuleResult.success(null);
        }
    }

    private static final class StatusCheck extends AbstractSingletonRuleLink<LoginRequest, String> {

        @Override
        protected RuleResult<String> apply(LoginRequest request, RuleContext context) {
            return request.active()
                    ? RuleResult.success("login-allowed")
                    : RuleResult.stop(600303, "account disabled", "login-blocked");
        }
    }

    private record LoginRequest(String account, String password, boolean active) {
    }
}
