package com.iisquare.fs.web.agent.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 编排节点实现接口 - 每种节点类型一个实现类，由 AgenticRunner 按节点类型分派。
 *
 * 实现类只关心「拿到配置，算出输出」：
 * 变量解析、模型调用、工具调用、容器内子流程调度等都通过 {@link AgenticNodeContext} 使用，
 * 节点实现不需要感知调度顺序、步骤日志与错误中断（这些由 AgenticRunner 统一处理）。
 */
public interface AgenticNodeHandler {

    /** 节点类型：与画布上节点 data.type 一致 */
    String type();

    /**
     * 执行节点
     * @param ctx 节点执行上下文（节点配置、运行入参、已执行节点输出、会话变量、历史对话）
     * @return 节点输出，写入运行输出表供下游节点引用
     */
    ObjectNode execute(AgenticNodeContext ctx) throws Exception;

}
