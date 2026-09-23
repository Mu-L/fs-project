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
 * HTTP 请求（节点类型：HTTP）
 */
@Service
public class HttpNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "HTTP";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return http(ctx, data);
    }

    protected ObjectNode http(AgenticNodeContext ctx, ObjectNode data) throws Exception {
        String method = data.at("/method").asText("GET");
        String url = ctx.text(data.at("/url").asText(""));
        Map<String, String> headers = ctx.stringMap(data.at("/headers"));
        Map<String, String> params = ctx.stringMap(data.at("/params"));
        ObjectNode body = ctx.object(data, "/body");
        String address = HttpUtil.buildUrlWithQueryString(url, params);
        ObjectNode request = DPUtil.objectNode();
        request.put("method", method);
        request.put("url", address);
        request.set("headers", DPUtil.toJSON(headers));
        ctx.lastRequest(request);
        String payload = null;
        if ("json".equals(body.at("/type").asText("")) || "raw".equals(body.at("/type").asText(""))) {
            payload = ctx.text(body.at("/content").asText(""));
            headers.putIfAbsent("Content-Type", "application/json;charset=UTF-8");
        } else if ("form-data".equals(body.at("/type").asText(""))) {
            // multipart/form-data：表单文本字段按标准分段发送
            String boundary = "----fs" + UUID.randomUUID().toString().replace("-", "");
            StringBuilder buffer = new StringBuilder();
            body.at("/form").fields().forEachRemaining(entry -> {
                buffer.append("--").append(boundary).append("\r\n");
                buffer.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n\r\n");
                buffer.append(ctx.text(entry.getValue().asText(""))).append("\r\n");
            });
            buffer.append("--").append(boundary).append("--\r\n");
            payload = buffer.toString();
            headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);
        } else if ("x-www-form-urlencoded".equals(body.at("/type").asText(""))) {
            List<String> items = new ArrayList<>();
            body.at("/form").fields().forEachRemaining(entry ->
                    items.add(entry.getKey() + "=" + ctx.text(entry.getValue().asText(""))));
            payload = DPUtil.implode("&", items);
            headers.putIfAbsent("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        }
        // 直连 HttpURLConnection：能拿到真实状态码与响应头（HttpUtil 失败时只返回 null，无法区分原因）
        HttpURLConnection conn = null;
        try {
            boolean withBody = !("GET".equals(method) || "DELETE".equals(method));
            conn = (HttpURLConnection) new URL(address).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(Math.max(1, data.at("/timeout").asInt(30)) * 1000);
            conn.setDoInput(true);
            conn.setDoOutput(withBody);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            if (withBody && null != payload) {
                byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                request.put("body", payload);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(bytes);
                    out.flush();
                }
            }
            int status = conn.getResponseCode();
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = null == stream ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            ObjectNode result = DPUtil.objectNode();
            result.put("body", response);
            result.put("statusCode", status);
            ObjectNode responseHeaders = DPUtil.objectNode();
            conn.getHeaderFields().forEach((key, values) -> {
                if (null != key && null != values) responseHeaders.put(key, DPUtil.implode(", ", values));
            });
            result.set("headers", responseHeaders);
            result.putArray("files");
            return result;
        } finally {
            if (null != conn) conn.disconnect();
        }
    }

}
