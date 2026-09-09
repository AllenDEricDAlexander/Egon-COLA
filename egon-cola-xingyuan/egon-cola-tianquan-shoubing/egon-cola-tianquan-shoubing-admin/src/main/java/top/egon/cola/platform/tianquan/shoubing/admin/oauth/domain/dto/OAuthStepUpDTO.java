package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto;

/**
 * Password proof used to raise the current USER token to STRONG authentication.
 */
public record OAuthStepUpDTO(String password) {
}
