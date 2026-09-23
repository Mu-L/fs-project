package com.iisquare.fs.web.agent.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 节点执行异常 - 携带失败前的部分输出。
 * 例如 ReAct 循环达到迭代上限时，虽然节点判定为失败，但之前各轮的工具调用过程
 * 需要随步骤一起记录，调试面板才能展示完整的调用链。
 */
public class AgenticNodeException extends RuntimeException {

    private final ObjectNode output;

    public AgenticNodeException(String message, ObjectNode output) {
        super(message);
        this.output = output;
    }

    /** 失败前的部分输出（可为 null） */
    public ObjectNode output() {
        return output;
    }

}
