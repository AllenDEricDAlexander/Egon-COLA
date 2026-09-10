/**
 * 提供 Gateway 读取 Tianquan-Shoubing 用户实时状态所需的运行时基础设施。
 * 当前实现建立专用单节点 Redisson 客户端，并从文件读取可选密码，避免与其他业务 Redis Bean 混淆。
 *
 * <p>Provides runtime infrastructure used by the Gateway to read current Tianquan-Shoubing user state. The
 * current implementation creates a dedicated single-server Redisson client and reads an optional
 * password from a file, avoiding ambiguity with unrelated application Redis beans.</p>
 */
package top.egon.cola.platform.tianquan.shoubing.yuheng.runtime;
