package com.iisquare.fs.web.agent.controller;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.sse.MaintainEmitter;
import com.iisquare.fs.web.core.rbac.MaintainControllerBase;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.agent.elasticsearch.KnowledgeChunkES;
import com.iisquare.fs.web.agent.service.KnowledgeChunkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RequestMapping("/maintain")
@RestController
public class MaintainController extends MaintainControllerBase {

    @Autowired
    KnowledgeChunkES chunkES;
    @Autowired
    KnowledgeChunkService chunkService;

    @GetMapping("/createChunk")
    public String createChunkAction(@RequestParam Map<String, Object> param) {
        boolean withAlias = !DPUtil.empty(param.get("withAlias"));
        return chunkES.create(withAlias);
    }

    /**
     * 重建检索块索引（修复历史数据缺失结构字段导致召回失效）
     * knowledgeId 为空时重建全部，执行过程参照 BI 维护任务以 SSE 输出
     */
    @RequestMapping("/reindexChunk")
    @Permission
    public SseEmitter reindexChunkAction(@RequestBody Map<String, Object> param, HttpServletRequest request,
            HttpServletResponse response) {
        MaintainEmitter emitter = new MaintainEmitter(request, response, 0L);
        return emitter.async(() -> chunkService.reindex(param, emitter));
    }

}
