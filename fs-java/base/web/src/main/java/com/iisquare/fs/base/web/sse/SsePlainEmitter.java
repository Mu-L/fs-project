package com.iisquare.fs.base.web.sse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseFixedEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SsePlainEmitter {

    public static final String EVENT_DATA_PREFIX = "data: ";
    SseFixedEmitter emitter;
    AtomicBoolean running = new AtomicBoolean(true);
    Runnable completionCallback = null;
    Consumer<Throwable> errorCallback = null;
    Runnable timeoutCallback = null;
    public Throwable failure = null; // 记录最终异常，一般为客户端断开连接

    public SsePlainEmitter() {
        emitter = new SseFixedEmitter();
        initialize();
    }

    /**
     * Params:
     * @param timeout timeout value in milliseconds
     *                By default, not set in which case the default configured in the MVC Java Config or the MVC namespace is used,
     *                or if that's not set, then the timeout depends on the default of the underlying server.
     */
    public SsePlainEmitter(Long timeout) {
        emitter = new SseFixedEmitter(timeout);
        initialize();
    }

    private void initialize() {
        emitter.onCompletion(() -> {
            running.set(false);
            if (null != completionCallback) {
                completionCallback.run();
            }
        });
        emitter.onError(throwable -> {
            this.failure = throwable;
            // 关闭浏览器或者前端主动Close会触发java.io.IOException: 你的主机中的软件中止了一个已建立的连接。
            running.set(false);
            if (null != errorCallback) {
                errorCallback.accept(throwable);
            }
        });
        emitter.onTimeout(() -> {
            // SSE长时间无数据返回，等待响应超时
            running.set(false);
            if (null != timeoutCallback) {
                timeoutCallback.run();
            }
        });
    }

    public SsePlainEventBuilder event() {
        return new SsePlainEventBuilder();
    }

    public SsePlainEmitter setMediaType(MediaType type) {
        emitter.setMediaType(type);
        return this;
    }

    public SsePlainEmitter setMediaType(CloseableHttpResponse response) {
        Header[] headers = response.getHeaders(HttpHeaders.CONTENT_TYPE);
        if (headers.length == 0) return this;
        emitter.setMediaType(MediaType.parseMediaType(headers[headers.length - 1].getValue()));
        return this;
    }

    public SsePlainEmitter send(SseEmitter.SseEventBuilder builder) {
        try {
            emitter.send(builder);
        } catch (IOException ignored) {}
        return this;
    }

    public SsePlainEmitter send(String line, boolean eventData) {
        if (eventData) {
            return data(line);
        } else {
            return line(line);
        }
    }

    public SsePlainEmitter line(String line) {
        try {
            emitter.send(event().line(line));
        } catch (IOException ignored) {}
        return this;
    }

    public SsePlainEmitter data(String line) {
        try {
            emitter.send(event().data(line));
        } catch (IOException ignored) {}
        return this;
    }

    public SsePlainEmitter error(String code, String message, String type, Object param, boolean eventData) {
        ObjectNode node = DPUtil.objectNode();
        ObjectNode error = node.putObject("error");
        error.put("message", message);
        error.put("type", type);
        error.replace("param", DPUtil.toJSON(param));
        error.put("code", code);
        return send(node.toString(), eventData);
    }

    public SsePlainEmitter onCompletion(Runnable callback) {
        completionCallback = callback;
        return this;
    }

    public SsePlainEmitter onError(Consumer<Throwable> callback) {
        errorCallback = callback;
        return this;
    }

    public SsePlainEmitter onTimeout(Runnable callback) {
        timeoutCallback = callback;
        return this;
    }

    public boolean isRunning() {
        return running.get();
    }

    public SsePlainEmitter abort() {
        running.set(false);
        return this;
    }

    public SseEmitter sync() {
        setMediaType(MediaType.APPLICATION_JSON);
        emitter.complete();
        return emitter;
    }

    public SseEmitter async(Runnable target) {
        new Thread(() -> {
            try {
                if (null != target) {
                    target.run();
                }
            } catch (Exception ignored) {} finally {
                emitter.complete();
            }
        }).start();
        return emitter;
    }

}
