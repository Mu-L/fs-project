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
 * 变量赋值（节点类型：VariableAssigner）
 */
@Service
public class VariableAssignerNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "VariableAssigner";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return assigner(ctx, data, ctx.outputs(), ctx.variables());
    }

    protected ObjectNode assigner(AgenticNodeContext ctx, ObjectNode data, Map<String, ObjectNode> outputs, Map<String, Object> context) {
        for (JsonNode item : data.at("/assignments")) {
            // 目标变量与变量引用是同一套规范：{{#容器标识.变量名#}} / {{#conversation.变量名#}}，
            // 兼容历史数据里直接存的裸取值（容器作用域键、会话变量名）
            String target = AgenticRuntime.referenceLiteral(item.at("/target").asText(""));
            if (DPUtil.empty(target)) continue;
            Object value = ctx.value("constant".equals(item.at("/source").asText("variable"))
                    ? item.at("/value").asText("")
                    : item.at("/variable").asText(""), outputs, context);
            String operation = item.at("/operation").asText("set");
            // 目标是容器作用域里的变量（循环变量、元素、索引）时写回作用域，容器内节点据此覆盖取值：
            // 引用里的容器标识定位到对应容器的作用域，嵌套容器里的同名变量也能区分
            ObjectNode scope = null;
            String name = target;
            int dot = target.lastIndexOf('.');
            if (dot > 0) {
                ObjectNode scoped = ctx.scopes().get(target.substring(0, dot));
                if (null != scoped) {
                    scope = scoped;
                    name = target.substring(dot + 1);
                }
            }
            // 历史数据：不带容器标识的裸变量名按作用域键兜底
            if (null == scope) scope = ctx.scopeOf(target);
            if (null != scope) {
                Object exists = scope.get(name);
                switch (operation) {
                    case "append":
                        scope.put(name, ctx.runtime().scalar(exists) + ctx.runtime().scalar(value));
                        break;
                    case "increment":
                        scope.put(name, DPUtil.parseInt(exists) + DPUtil.parseInt(value));
                        break;
                    case "decrement":
                        scope.put(name, DPUtil.parseInt(exists) - DPUtil.parseInt(value));
                        break;
                    case "clear":
                        scope.put(name, "");
                        break;
                    default:
                        scope.set(name, DPUtil.toJSON(value));
                        break;
                }
                continue;
            }
            Object exists = context.get(name);
            switch (operation) {
                case "append":
                    context.put(name, ctx.runtime().scalar(exists) + ctx.runtime().scalar(value));
                    break;
                case "increment":
                    context.put(name, DPUtil.parseInt(exists) + DPUtil.parseInt(value));
                    break;
                case "decrement":
                    context.put(name, DPUtil.parseInt(exists) - DPUtil.parseInt(value));
                    break;
                case "clear":
                    context.remove(name);
                    break;
                default:
                    context.put(name, value);
                    break;
            }
        }
        ObjectNode result = DPUtil.objectNode();
        result.set("conversation", DPUtil.toJSON(context));
        return result;
    }

}
