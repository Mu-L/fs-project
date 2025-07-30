package com.iisquare.fs.web.oa.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import com.iisquare.fs.web.oa.entity.Workflow;
import com.iisquare.fs.web.oa.service.ApproveService;
import com.iisquare.fs.web.oa.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
public class WorkflowController extends PermitControllerBase {

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private ApproveService approveService;
    @Autowired
    private MemberRpc memberRpc;

    @RequestMapping("/process")
    @Permission
    public String processAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String processInstanceId = DPUtil.trim(DPUtil.parseString(param.get("processInstanceId")));
        String taskId = DPUtil.trim(DPUtil.parseString(param.get("taskId")));
        Map<String, Object> result = approveService.process(
                processInstanceId, taskId, rbacService.currentInfo(request), DPUtil.objectNode().put("viewable", true));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/publish")
    @Permission
    public String publishAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Map<String, Object> result = workflowService.deployment(id, rbacService.uid(request));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        boolean withDeployment = !DPUtil.empty(param.get("withDeployment"));
        boolean withForm = !DPUtil.empty(param.get("withForm"));
        boolean withFormDetail = !DPUtil.empty(param.get("withFormDetail"));
        boolean withFormRemote = !DPUtil.empty(param.get("withFormRemote"));
        Workflow info = workflowService.info(id, withDeployment, withForm, withFormDetail, withFormRemote);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = workflowService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withDeploymentInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/searchHistory")
    @Permission
    public String searchHistoryAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Map<?, ?> result = workflowService.searchHistory(param, DPUtil.buildMap("withUserInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/searchDeployment")
    @Permission
    public String searchDeploymentAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = workflowService.searchDeployment(param, DPUtil.buildMap());
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/reject")
    @Permission
    public String rejectAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String processInstanceId = DPUtil.trim(DPUtil.parseString(param.get("processInstanceId")));
        boolean local = !DPUtil.empty(param.get("local"));
        String reason = DPUtil.trim(DPUtil.parseString(param.get("reason")));
        ObjectNode audit = DPUtil.objectNode().put("local", local).put("message", reason);
        Map<String, Object> result = approveService.reject(processInstanceId, null, audit, rbacService.currentInfo(request));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/deleteProcessInstance")
    @Permission
    public String deleteProcessInstanceAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String processInstanceId = DPUtil.trim(DPUtil.parseString(param.get("processInstanceId")));
        String reason = DPUtil.trim(DPUtil.parseString(param.get("reason")));
        Map<String, Object> result = workflowService.deleteProcessInstance(processInstanceId, reason);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/deleteHistoricProcessInstance")
    @Permission
    public String deleteHistoricProcessInstanceAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String processInstanceId = DPUtil.trim(DPUtil.parseString(param.get("processInstanceId")));
        Map<String, Object> result = workflowService.deleteHistoricProcessInstance(processInstanceId);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/activateProcessInstance")
    @Permission
    public String activateProcessInstanceAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String processInstanceId = DPUtil.trim(DPUtil.parseString(param.get("processInstanceId")));
        Map<String, Object> result = workflowService.toggleProcessInstance(processInstanceId, true);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/suspendProcessInstance")
    @Permission
    public String suspendProcessInstanceAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String processInstanceId = DPUtil.trim(DPUtil.parseString(param.get("processInstanceId")));
        Map<String, Object> result = workflowService.toggleProcessInstance(processInstanceId, false);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/deleteDeployment")
    @Permission
    public String deleteDeploymentAction(@RequestBody Map<?, ?> param) {
        String id = DPUtil.parseString(param.get("id"));
        boolean cascade = !DPUtil.empty(param.get("cascade"));
        workflowService.deleteDeployment(id, cascade);
        return ApiUtil.echoResult(0, null, id);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = workflowService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/candidateInfos")
    @Permission("modify")
    public String candidateInfosAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return memberRpc.post("/rbac/infos", param);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = workflowService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", workflowService.status("default"));
        model.put("finishStatus", workflowService.finishStatus("default"));
        model.put("deleteStatus", workflowService.deleteStatus("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
