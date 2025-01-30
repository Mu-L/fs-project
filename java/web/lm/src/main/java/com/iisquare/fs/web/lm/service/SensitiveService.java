package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import com.iisquare.fs.web.lm.dao.SensitiveDao;
import com.iisquare.fs.web.lm.entity.Sensitive;
import com.iisquare.fs.web.lm.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SensitiveService extends JPAServiceBase {

    @Autowired
    private SensitiveDao sensitiveDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    MemberRpc memberRpc;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "监测");
        status.put(2, "预警");
        status.put(3, "拦截");
        return status;
    }

    public JsonNode risk() {
        Map<Object, Object> params = DPUtil.buildMap("dictionary", "ai-risk");
        return RpcUtil.data(memberRpc.post("/dictionary/options", params), true);
    }

    public Sensitive info(Integer id) {
        return info(sensitiveDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String content = DPUtil.trim(DPUtil.parseString(param.get("content")));
        if(DPUtil.empty(content)) return ApiUtil.result(1001, "关键词内容异常", content);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Sensitive info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Sensitive();
        }
        info.setContent(content);
        info.setRisk(DPUtil.implode(",", DPUtil.parseIntList(param.get("risk"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(sensitiveDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(sensitiveDao, param, (root, query, cb) -> {
            SpecificationHelper<Sensitive> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("content").functionFindInSet("risk");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            String[] risk = DPUtil.explode(",", node.at("/risk").asText());
            node.replace("risk", DPUtil.toJSON(risk));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(sensitiveDao, ids);
    }

}
