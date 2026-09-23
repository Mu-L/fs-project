package com.iisquare.fs.web.agent.core;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * 子流程调度接口 - 由 AgenticRunner 实现，供容器节点（迭代、循环）执行容器内部的子图。
 * 节点实现类只声明「要执行容器内的节点」，具体顺序、步骤日志与错误中断由调度器负责。
 */
public interface AgenticScheduler {

    /**
     * 执行容器内的子图：以容器为父节点、在容器内没有入边的节点作为入口，按连线顺序执行
     * @param containerId 容器节点标识
     * @param outputs     节点输出表（容器内节点写入同一份表）
     * @param variables   会话变量（可写）
     * @param history     历史对话
     * @param iteration   当前第几次执行（从 0 开始），写入步骤日志便于区分
     */
    void runChildren(String containerId, Map<String, ObjectNode> outputs, Map<String, Object> variables,
                     ArrayNode history, int iteration) throws Exception;

}
