package com.iisquare.fs.web.agent.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticNodeContext;
import com.iisquare.fs.web.agent.core.AgenticNodeHandler;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 输出图表（节点类型：Chart）
 * 按用户问题与上游上下文自动决定是否绘图，并由模型给出图表定义（图表类型与展示数据都由模型决定），随最终回复一起展示。
 * 无需配置图表类型与图表要求：节点自动收集开始节点的用户输入与上游节点的输出（大语言模型的工具调用结果等）。
 * 与「大语言模型」节点分开的好处：图表协议是内置提示词、不占用回复正文（正文不含 JSON），
 * 且回复由模板等其它节点拼装时同样能挂上图表。
 * 节点输出：{ id, placeholder, type, title, source, categories, series, text }，
 * 前端按 categories + series 渲染表格/柱状/折线/饼图；placeholder 是回复正文里的图表占位符。
 */
@Service
public class ChartNodeHandler implements AgenticNodeHandler {

    /** 内置提示词：模型自己理解用户问题与上下文数据，决定图表类型并归纳出展示数据 */
    protected static final String PROMPT = """
            你是数据可视化助手：根据「用户问题」与「上下文数据」判断是否需要绘图，并给出一张图表的定义。
            只输出一个 JSON 对象，不要输出其它文字或代码块：
            {"type":"bar","title":"图表标题","source":"数据来源","categories":["分类一","分类二"],"series":[{"name":"系列名","data":[1,2]}]}
            - 图表类型由你按数据与问题选择：对比用 bar（柱状）、趋势用 line（折线）、占比用 pie（饼图）、明细用 table（表格）；
            - 数据必须来自上下文数据，并按问题口径提取与归纳（如分组汇总、排序取前 10 项），不要臆造；
            - title 说明统计口径（如「各部门人数」），source 写数据来源（如数据集或主题名称，取不到就留空）；
            - categories 为分类取值，series 为数值系列，data 与 categories 一一对应；分类不超过 20 项、系列不超过 2 个；
            - 数值不带单位与千分位；用户问题与数据无关、或上下文里没有可绘图的数据时，返回 {"type":"none","reason":"原因"}。""";

    /** 图表类型字典：与前端图表组件一致 */
    protected static final Map<String, String> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put("bar", "柱状");
        TYPES.put("line", "折线");
        TYPES.put("pie", "饼图");
        TYPES.put("table", "表格");
    }

    @Override
    public String type() {
        return "Chart";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        ctx.checkGateway();
        String context = context(ctx);
        if (DPUtil.empty(context)) {
            throw new IllegalStateException("没有可用的上下文数据：请把用户输入与上游数据节点连接到「输出图表」节点");
        }
        ObjectNode json = DPUtil.objectNode();
        json.put("model", data.at("/model").asText(""));
        json.put("stream", false);
        if (data.at("/temperatureEnabled").asBoolean(false)) {
            json.put("temperature", data.at("/temperature").asDouble(0));
        }
        ctx.runtime().thinking(json, data);
        ArrayNode messages = json.putArray("messages");
        // 系统提示词：内置图表协议 + 用户补充（补充内容在前，协议在后，避免被覆盖）
        String system = ctx.text(data.at("/systemPrompt").asText(""));
        system = DPUtil.empty(system) ? PROMPT : system + "\n\n" + PROMPT;
        messages.addObject().put("role", "system").put("content", system);
        // 多轮对话：与其它模型节点一致，按节点的记忆窗口把历史消息插在系统提示词与本次输入之间
        // （开启「工具链」时历史轮次的工具调用也会带进来，便于按上一轮的数据继续画图）
        for (JsonNode item : ctx.historyMessages(data)) messages.add(item);
        // 用户消息：自动汇总的用户问题与上下文数据，图表类型与数据由模型自行判断
        ctx.userMessage(messages, "上下文数据：\n" + context, data);
        ctx.lastRequest(json.deepCopy());
        JsonNode result = ctx.completion(json);
        String content = result.at("/choices/0/message/content").asText("");
        ObjectNode chart = chart(content);
        if (null == chart) {
            throw new IllegalStateException("模型未按要求返回图表定义：" + abbreviate(content));
        }
        chart.put("text", content);
        // 回复正文里的图表占位符：结束节点引用它即可把图表渲染在指定位置，不引用则追加在回复末尾
        chart.put("id", ctx.id());
        chart.put("placeholder", placeholder(ctx.id()));
        // 本轮是否真的产出了图表：模型判定无需绘图（type=none）时为 false，
        // 下游节点/条件分支可直接引用该判定结果
        chart.put("hasChart", !chart.at("/series").isEmpty());
        return chart;
    }

    /** 图表占位符：不会与变量、Jinja 模板语法冲突，前端按它把回复分段渲染 */
    public static String placeholder(String nodeId) {
        return "[[chart:" + DPUtil.parseString(nodeId) + "]]";
    }

    /** 去掉正文里的图表占位符：历史上下文只带模型该看的文字（图表不是模型的上下文内容） */
    public static String stripPlaceholder(String text) {
        String content = DPUtil.parseString(text);
        if (DPUtil.empty(content)) return content;
        return content.replaceAll("\\[\\[chart:[^\\[\\]]+\\]\\]", "").trim();
    }

    /**
     * 上下文数据：默认只取本轮的记录——用户问题（开始节点入参，去掉文件列表）
     * 与上游大语言模型节点的本轮输出（回复文本 + 工具调用明细，工具结果里就是真实数据）；
     * 流程里没有大语言模型节点时（如 HTTP 直接接图表），才退回使用其它上游节点的输出。
     * 不做长度截断；历史轮次由节点的记忆窗口统一处理，与其它模型节点一致。
     */
    protected String context(AgenticNodeContext ctx) {
        ArrayNode items = DPUtil.arrayNode();
        ArrayNode fallback = DPUtil.arrayNode();
        boolean withModel = false;
        for (String id : ancestors(ctx)) {
            ObjectNode node = ctx.nodeMap().get(id);
            ObjectNode output = ctx.outputs().get(id);
            if (null == node || null == output) continue;
            String type = node.at("/data/type").asText("");
            // 图表节点之间不互相传递，避免串联出重复图表
            if (type().equals(type)) continue;
            ObjectNode picked = pick(type, output);
            if (picked.isEmpty()) continue;
            if ("Start".equals(type) || "LLM".equals(type)) {
                withModel = withModel || "LLM".equals(type);
                collect(items, node, type, picked);
            } else {
                collect(fallback, node, type, picked);
            }
        }
        // 默认只用用户问题与大语言模型本轮的记录（含工具调用），其余上游节点仅在缺少模型节点时兜底
        if (!withModel) items.addAll(fallback);
        if (items.isEmpty()) return "";
        return DPUtil.stringify(items);
    }

    /** 收集一条上下文：节点名称 + 节点类型 + 挑出的数据 */
    protected void collect(ArrayNode target, ObjectNode node, String type, ObjectNode picked) {
        ObjectNode item = target.addObject();
        item.put("节点", node.at("/data/name").asText(node.at("/id").asText("")));
        item.put("类型", type);
        item.set("数据", picked);
    }

    /** 上游节点：从当前节点沿连线反向遍历（含跨层级的节点，按由近到远的顺序） */
    protected List<String> ancestors(AgenticNodeContext ctx) {
        Map<String, List<String>> sources = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, String>>> entry : ctx.edgeMap().entrySet()) {
            for (Map<String, String> edge : entry.getValue()) {
                sources.computeIfAbsent(edge.get("target"), key -> new ArrayList<>()).add(entry.getKey());
            }
        }
        List<String> result = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>(sources.getOrDefault(ctx.id(), new ArrayList<>()));
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (DPUtil.empty(id) || !visited.add(id)) continue;
            result.add(id);
            queue.addAll(sources.getOrDefault(id, new ArrayList<>()));
        }
        return result;
    }

    /** 上游输出里与绘图相关的字段：开始节点去掉文件列表，模型节点只保留本轮回复文本与工具调用明细 */
    protected ObjectNode pick(String type, ObjectNode output) {
        ObjectNode picked = DPUtil.objectNode();
        if ("Start".equals(type)) {
            output.properties().forEach(entry -> {
                if (!"files".equals(entry.getKey())) picked.set(entry.getKey(), entry.getValue());
            });
        } else if ("LLM".equals(type)) {
            picked.set("text", output.at("/text"));
            picked.set("calls", output.at("/calls"));
        } else if ("Knowledge".equals(type)) {
            picked.set("text", output.at("/text"));
            picked.set("result", output.at("/result"));
        } else if ("HTTP".equals(type)) {
            picked.set("body", output.at("/body"));
        } else {
            output.properties().forEach(entry -> picked.set(entry.getKey(), entry.getValue()));
        }
        return picked;
    }

    /**
     * 模型输出 → 图表定义：容错取出第一个 JSON 对象（模型可能包一层代码块或加说明文字），
     * 结构不合法时返回 null，由调用方按节点异常处理。
     */
    protected ObjectNode chart(String content) {
        String text = DPUtil.parseString(content).trim();
        int begin = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (begin < 0 || end <= begin) return null;
        JsonNode json = DPUtil.parseJSON(text.substring(begin, end + 1));
        if (null == json || !json.isObject()) return null;
        String type = json.at("/type").asText("bar");
        // 模型判断无需绘图：返回空图表，运行结果里不含图表，回复照常展示文本
        if ("none".equals(type)) {
            ObjectNode empty = DPUtil.objectNode();
            empty.put("type", "none");
            empty.put("title", "");
            empty.put("source", "");
            empty.put("reason", json.at("/reason").asText(""));
            empty.putArray("categories");
            empty.putArray("series");
            return empty;
        }
        JsonNode categories = json.at("/categories");
        JsonNode series = json.at("/series");
        if (!categories.isArray() || categories.isEmpty() || !series.isArray() || series.isEmpty()) return null;
        ObjectNode chart = DPUtil.objectNode();
        chart.put("type", TYPES.containsKey(type) ? type : "bar");
        chart.put("title", json.at("/title").asText(""));
        chart.put("source", json.at("/source").asText(""));
        ArrayNode labels = chart.putArray("categories");
        for (JsonNode item : categories) {
            // 防御：模型偶尔给出过长的清单，图表与表格都按 200 项截断
            if (labels.size() >= 200) break;
            labels.add(item.isValueNode() ? item.asText("") : DPUtil.stringify(item));
        }
        ArrayNode items = chart.putArray("series");
        for (JsonNode item : series) {
            // 与前端取值规则一致：最多展示两个系列
            if (items.size() >= 2) break;
            JsonNode data = item.at("/data");
            if (!data.isArray() || data.isEmpty()) continue;
            ObjectNode node = items.addObject();
            node.put("name", DPUtil.empty(item.at("/name").asText("")) ? "系列" + (items.size()) : item.at("/name").asText(""));
            ArrayNode values = node.putArray("data");
            for (JsonNode value : data) {
                if (values.size() >= labels.size()) break;
                values.add(number(value));
            }
            // 数据长度与分类轴对齐，避免图表错位
            while (values.size() < labels.size()) values.add(0);
        }
        return items.isEmpty() ? null : chart;
    }

    /** 图表数值：非数值或缺失按 0 处理（模型偶尔会把数值写成带单位的文本） */
    protected double number(JsonNode value) {
        if (null == value || value.isNull()) return 0;
        if (value.isNumber()) return value.asDouble();
        try {
            return Double.parseDouble(value.asText("").trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 异常信息里带上模型原文（截断），便于在调试面板定位是提示词还是模型的问题 */
    protected String abbreviate(String content) {
        String text = DPUtil.parseString(content).trim();
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }

}
