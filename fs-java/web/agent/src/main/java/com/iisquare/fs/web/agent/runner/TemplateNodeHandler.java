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
 * Jinja2 模板转换（节点类型：Template）
 */
@Service
public class TemplateNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "Template";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return template(ctx, data);
    }

    protected ObjectNode template(AgenticNodeContext ctx, ObjectNode data) {
        ObjectNode result = DPUtil.objectNode();
        String name = data.at("/outputName").asText("output");
        String value = render(ctx, data);
        result.put(name, value);
        return result;
    }

    protected String render(AgenticNodeContext ctx, ObjectNode data) {
        String template = data.at("/template").asText("");
        if (DPUtil.empty(template)) return "";
        String plain = ctx.text(template);
        Map<String, Object> values = new LinkedHashMap<>();
        for (JsonNode item : data.at("/inputs")) {
            String name = item.at("/name").asText("");
            if (DPUtil.empty(name)) continue;
            Object value = ctx.value(item.at("/variable").asText(""));
            // JsonNode 转成 Map/List/标量，交给 Jinja2 上下文使用
            values.put(name, value instanceof JsonNode ? DPUtil.toJSON((JsonNode) value, Object.class) : value);
        }
        try {
            return new Jinjava().render(plain, values);
        } catch (Exception e) {
            return plain;
        }
    }

}
