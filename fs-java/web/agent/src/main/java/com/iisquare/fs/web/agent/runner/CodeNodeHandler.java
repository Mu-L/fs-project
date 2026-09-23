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
 * 代码执行（JavaScript 引擎）（节点类型：Code）
 */
@Service
public class CodeNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "Code";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return code(ctx, data, ctx.outputs());
    }

    protected ObjectNode code(AgenticNodeContext ctx, ObjectNode data, Map<String, ObjectNode> outputs) throws Exception {
        ObjectNode args = DPUtil.objectNode();
        for (JsonNode item : data.at("/inputs")) {
            String name = item.at("/name").asText("");
            if (DPUtil.empty(name)) continue;
            args.set(name, DPUtil.toJSON(ctx.value(item.at("/variable").asText(""))));
        }
        String script = "var args = " + DPUtil.stringify(args) + ";\n"
                + data.at("/code").asText("") + "\nJSON.stringify(main(args));";
        JsonNode result = DPUtil.parseJSON(evaluate(ctx, script, Math.max(1, data.at("/timeout").asInt(30))));
        ObjectNode value = DPUtil.objectNode();
        for (JsonNode item : data.at("/outputs")) {
            String name = item.at("/name").asText("");
            if (DPUtil.empty(name)) continue;
            value.set(name, null == result ? DPUtil.toJSON("") : result.at("/" + name));
        }
        return value;
    }

    protected String evaluate(AgenticNodeContext ctx, String script, int seconds) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");
        if (null == engine) engine = manager.getEngineByName("JavaScript");
        if (null == engine) throw new IllegalStateException("服务端未提供 JavaScript 引擎，无法执行代码节点");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // 引擎变量可能被重新赋值，这里用局部终态变量供 lambda 引用
            final ScriptEngine runtime = engine;
            Future<Object> future = pool.submit(() -> runtime.eval(script));
            return DPUtil.parseString(future.get(seconds, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            throw new IllegalStateException("代码执行超时（" + seconds + " 秒）");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new IllegalStateException("代码执行失败：" + ctx.message(null == cause ? e : cause));
        } finally {
            pool.shutdownNow();
        }
    }

}
