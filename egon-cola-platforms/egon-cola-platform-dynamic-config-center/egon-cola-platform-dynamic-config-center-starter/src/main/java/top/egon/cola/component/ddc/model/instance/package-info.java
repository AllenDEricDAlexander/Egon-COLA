/**
 * DDC 实例身份与运行阶段值对象，描述实例是谁以及当前处于何种生命周期状态。
 * 本包不生成身份、不持有可变状态；{@code service.lifecycle} 创建并推进这些不可变模型。
 *
 * <p>Value objects describing DDC instance identity and lifecycle phase. Identity generation and
 * mutable state are excluded; {@code service.lifecycle} creates and advances these immutable models.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.model.instance;

import org.springframework.lang.NonNullApi;
