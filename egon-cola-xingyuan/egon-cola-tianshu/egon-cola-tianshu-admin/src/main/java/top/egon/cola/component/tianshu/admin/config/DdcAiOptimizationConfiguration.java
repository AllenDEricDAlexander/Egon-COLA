package top.egon.cola.component.tianshu.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 反馈与优化通道装配；模型、作业与执行器在通道开启时装配。
 * / Wiring for the AI feedback and optimization channel; model, jobs and executor are created when enabled.
 */
@Configuration(value = "ddcAiOptimizationConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(DdcAiOptimizationProperties.class)
public class DdcAiOptimizationConfiguration {
}
