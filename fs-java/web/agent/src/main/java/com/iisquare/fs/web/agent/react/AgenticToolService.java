package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.core.AgenticToolInvoker;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReAct 工具注册中心 - 按 kind 分派到对应的工具实现：
 * definitions() 生成模型可见的 function 定义，invoke() 按方法名找到工具项并执行。
 */
@Service
public class AgenticToolService implements AgenticToolInvoker {

    @Autowired
    List<AgenticTool> toolList;
    private final Map<String, AgenticTool> tools = new LinkedHashMap<>();

    /** 建立 kind → 实现 的索引（kind 缺省为 method，兼容历史数据） */
    @PostConstruct
    protected void registry() {
        if (null == toolList) return;
        for (AgenticTool tool : toolList) tools.put(tool.kind(), tool);
    }

    /** 工具定义清单：逐项生成 function 定义，不可用的配置跳过 */
    public ArrayNode definitions(JsonNode items) {
        ArrayNode result = DPUtil.arrayNode();
        for (JsonNode item : items) {
            if (item.has("enabled") && !item.at("/enabled").asBoolean(true)) continue;
            String kind = item.at("/kind").asText("method");
            AgenticTool tool = tools.get(DPUtil.empty(kind) ? "method" : kind);
            if (null == tool) continue;
            ObjectNode definition = tool.definition(item);
            if (null == definition) continue;
            result.add(definition);
        }
        return result;
    }

    /** 执行工具调用：按方法名匹配工具项后交给对应实现 */
    public Object invoke(AgenticRuntime runtime, JsonNode items, String methodName, Map<String, Object> args,
                         Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        for (JsonNode item : items) {
            if (item.has("enabled") && !item.at("/enabled").asBoolean(true)) continue;
            String kind = item.at("/kind").asText("method");
            if (DPUtil.empty(kind)) kind = "method";
            AgenticTool tool = tools.get(kind);
            if (null == tool) continue;
            String name = "method".equals(kind) ? item.at("/method").asText("") : item.at("/name").asText("");
            if (!methodName.equals(name)) continue;
            try {
                return tool.invoke(runtime, item, args, outputs, variables);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(DPUtil.parseString(e.getMessage()));
            }
        }
        throw new IllegalStateException("未找到工具方法：" + methodName);
    }

}
