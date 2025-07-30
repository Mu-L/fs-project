package com.iisquare.fs.web.lucene.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lucene.entity.Dictionary;
import com.iisquare.fs.web.lucene.helper.SCELHelper;
import com.iisquare.fs.web.lucene.service.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dictionary")
public class DictionaryController extends PermitControllerBase {

    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private DictionaryService dictionaryService;

    @GetMapping("/plain")
    public String plainAction(@RequestParam Map<String, Object> param, HttpServletResponse response) {
        String name = DPUtil.trim(DPUtil.parseString(param.get("catalogue")));
        name += "-" + DPUtil.trim(DPUtil.parseString(param.get("type")));
        response.setContentType("text/plain;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + name + ".dict");
        return dictionaryService.plain(param);
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Dictionary info = dictionaryService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = dictionaryService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withTypeText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = dictionaryService.saveAll(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = dictionaryService.delete(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/unique")
    @Permission("delete")
    public String uniqueAction(@RequestBody Map<?, ?> param) {
        String catalogue = DPUtil.trim(DPUtil.parseString(param.get("catalogue")));
        if (DPUtil.empty(catalogue)) return ApiUtil.echoResult(1001, "词库目录不能为空", catalogue);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if (DPUtil.empty(type)) return ApiUtil.echoResult(1001, "词库分类不能为空", type);
        Integer result = dictionaryService.unique(catalogue, type);
        return ApiUtil.echoResult(null == result ? 500 : 0, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("type", dictionaryService.type());
        model.put("sort", dictionaryService.sort());
        return ApiUtil.echoResult(0, null, model);
    }

    @PostMapping("/scel")
    public String scelAction(HttpServletRequest request, @RequestPart("file") MultipartFile file) {
        if(null == file) {
            return ApiUtil.echoResult(1003, "获取文件句柄失败", null);
        }
        File tmp = null;
        try {
            tmp = File.createTempFile("sougou", "scel");
        } catch (IOException e) {
            tmp.delete();
            return ApiUtil.echoResult(1004, "创建临时文件失败", e.getMessage());
        }
        try {
            file.transferTo(tmp);
        } catch (IOException e) {
            tmp.delete();
            return ApiUtil.echoResult(1005, "写入临时文件失败", e.getMessage());
        }
        Map<String, LinkedList<String>> wordList;
        try {
            wordList = SCELHelper.getInstance(tmp).parse().wordList();
        } catch (Exception e) {
            tmp.delete();
            return ApiUtil.echoResult(1006, "解析文件失败", e.getMessage());
        }
        tmp.delete();
        return ApiUtil.echoResult(0, null, wordList);
    }

}
