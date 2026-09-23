package com.iisquare.fs.web.agent.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.web.agent.entity.Tool;
import com.iisquare.fs.web.agent.entity.ToolMethod;
import com.iisquare.fs.web.agent.core.AgenticNodeContext;
import com.iisquare.fs.web.agent.core.AgenticNodeHandler;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.tool.DocumentParser;
import com.hubspot.jinjava.Jinjava;
import com.iisquare.fs.web.core.rpc.FileRpc;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 列表操作（过滤、排序、截取）（节点类型：ListOperator）
 */
@Service
public class ListOperatorNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "ListOperator";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return listOperator(ctx, data);
    }

    protected ObjectNode listOperator(AgenticNodeContext ctx, ObjectNode data) {
        List<Object> items = new ArrayList<>(ctx.list(ctx.value(data.at("/input").asText(""))));
        ObjectNode filter = ctx.object(data, "/filter");
        items.removeIf(item -> !matchList(ctx, item, filter));
        List<Map<String, Object>> sorts = new ArrayList<>();
        for (JsonNode item : data.at("/sorts")) {
            if (DPUtil.empty(item.at("/variable").asText(""))) continue;
            Map<String, Object> sort = new LinkedHashMap<>();
            sort.put("field", item.at("/variable").asText(""));
            sort.put("desc", "desc".equals(item.at("/order").asText("asc")));
            sorts.add(sort);
        }
        if (!sorts.isEmpty()) {
            items.sort((left, right) -> {
                for (Map<String, Object> sort : sorts) {
                    int result = ctx.compare(ctx.field(left, DPUtil.parseString(sort.get("field"))), ctx.field(right, DPUtil.parseString(sort.get("field"))));
                    if (0 != result) return Boolean.TRUE.equals(sort.get("desc")) ? -result : result;
                }
                return 0;
            });
        }
        ObjectNode limit = ctx.object(data, "/limit");
        String type = limit.at("/type").asText("all");
        int size = Math.max(1, limit.at("/size").asInt(10));
        if ("first".equals(type) && items.size() > size) items = new ArrayList<>(items.subList(0, size));
        if ("last".equals(type) && items.size() > size) items = new ArrayList<>(items.subList(items.size() - size, items.size()));
        ObjectNode result = DPUtil.objectNode();
        result.set(data.at("/outputName").asText("result"), DPUtil.toJSON(items));
        result.put("total", items.size());
        result.put("first", items.isEmpty() ? null : DPUtil.toJSON(items.get(0)));
        return result;
    }

    protected boolean matchList(AgenticNodeContext ctx, Object item, ObjectNode filter) {
        List<JsonNode> conditions = new ArrayList<>();
        filter.at("/conditions").forEach(conditions::add);
        if (conditions.isEmpty()) return true;
        boolean any = "or".equals(filter.at("/logic").asText("and"));
        for (JsonNode condition : conditions) {
            boolean current = ctx.compareWith(DPUtil.parseString(ctx.field(item, condition.at("/variable").asText(""))),
                    condition.at("/operator").asText("eq"), condition.at("/value").asText(""));
            if (any && current) return true;
            if (!any && !current) return false;
        }
        return !any;
    }

}
