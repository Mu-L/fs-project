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
 * 变量聚合器（节点类型：VariableAggregator）
 */
@Service
public class VariableAggregatorNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "VariableAggregator";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return aggregator(ctx, data);
    }

    protected ObjectNode aggregator(AgenticNodeContext ctx, ObjectNode data) {
        ObjectNode result = DPUtil.objectNode();
        for (JsonNode group : data.at("/groups")) {
            String name = group.at("/name").asText("");
            if (DPUtil.empty(name)) continue;
            Object value = null;
            for (JsonNode item : group.at("/variables")) {
                value = ctx.value(item.at("/variable").asText(""));
                if (!ctx.blank(value)) break;
            }
            result.set(name, DPUtil.toJSON(value));
        }
        return result;
    }

}
