package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.core.rpc.BIRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 数据主题（kind = theme） - 内部调用（对应 /bi/data/theme 配置的主题）：
 * - 不传 sql：返回主题的数据字典（数据集 + 字段 + 关联关系），供模型据此编写查询；
 * - 传 sql：按主题范围内已发布的数据集执行查询（表名用数据集名称），返回查询结果。
 *
 * 调用走 BI 服务 RPC，FeignInterceptor 会透传当前登录用户的 x-auth-token，
 * BI 侧据此替换与登录用户相关的变量。
 */
@Service
public class ThemeTool implements AgenticTool {

    @Autowired
    BIRpc biRpc;

    @Override
    public String kind() {
        return "theme";
    }

    @Override
    public ObjectNode definition(JsonNode item) {
        String name = item.at("/name").asText("");
        if (DPUtil.empty(name)) return null;
        ArrayNode params = DPUtil.arrayNode();
        params.add(AgenticTool.parameter("sql", false, AgenticTool.schema("string",
                "针对主题内数据集编写的查询 SQL（表名用数据集名称）；不传则返回主题的数据字典")));
        params.add(AgenticTool.parameter("limit", false, AgenticTool.schema("integer", "返回的最大行数，默认 100")));
        return AgenticTool.definition(name, item.at("/description").asText(""), params, item);
    }

    @Override
    public Object invoke(AgenticRuntime runtime, JsonNode item, Map<String, Object> args,
                         Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        int themeId = item.at("/themeId").asInt(0);
        if (themeId < 1) throw new IllegalStateException("数据主题工具未选择主题");
        String sql = DPUtil.parseString(args.get("sql"));
        ObjectNode dictionary = dictionary(themeId);
        if (DPUtil.empty(sql)) return dictionary;
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("sql", sql);
        param.put("themeId", themeId); // 供数据查询日志归类到所属数据主题
        param.put("themeName", dictionary.at("/themeName").asText(""));
        param.put("limit", null == args.get("limit") || DPUtil.parseInt(args.get("limit")) < 1
                ? 100 : Math.min(DPUtil.parseInt(args.get("limit")), 10000));
        JsonNode data = RpcUtil.data(biRpc.get("/dataset/query", param), false);
        ObjectNode value = DPUtil.objectNode();
        value.put("themeId", themeId);
        value.put("themeName", dictionary.at("/themeName").asText(""));
        value.put("sql", sql);
        value.set("data", null == data ? DPUtil.objectNode() : data);
        return value;
    }

    /** 主题数据字典：主题基础信息 + 数据集（名称、字段）+ 关联关系 */
    public ObjectNode dictionary(int themeId) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("id", themeId);
        JsonNode info = RpcUtil.data(biRpc.post("/dataTheme/info", param), false);
        if (null == info || info.isNull() || info.isMissingNode()) {
            throw new IllegalStateException("数据主题不存在或无权访问：" + themeId);
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("themeId", themeId);
        result.put("themeName", info.at("/name").asText(""));
        result.put("description", info.at("/description").asText(""));
        // 数据集名称：主题详情里带了数据集信息时直接取用
        Map<String, String> datasetNames = new LinkedHashMap<>();
        for (JsonNode dataset : info.at("/datasets")) {
            datasetNames.put(dataset.at("/id").asText(""), dataset.at("/name").asText(""));
        }
        Set<Integer> datasetIds = new LinkedHashSet<>();
        for (JsonNode item : info.at("/content/datasetIds")) {
            int id = item.asInt(0);
            if (id > 0) datasetIds.add(id);
        }
        for (JsonNode item : info.at("/datasetIds")) {
            int id = item.asInt(0);
            if (id > 0) datasetIds.add(id);
        }
        ArrayNode datasets = result.putArray("datasets");
        for (Integer id : datasetIds) {
            ObjectNode dataset = datasets.addObject();
            dataset.put("id", id);
            Map<String, Object> columnsParam = new LinkedHashMap<>();
            columnsParam.put("id", id);
            JsonNode columns = RpcUtil.data(biRpc.post("/dataset/columns", columnsParam), false);
            String name = datasetNames.get(String.valueOf(id));
            if (DPUtil.empty(name) && null != columns) name = columns.at("/name").asText("");
            dataset.put("name", DPUtil.empty(name) ? String.valueOf(id) : name);
            dataset.set("columns", null == columns ? DPUtil.arrayNode() : columns.at("/columns"));
        }
        result.set("relations", info.at("/content/relations"));
        return result;
    }

}
