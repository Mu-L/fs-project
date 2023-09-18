package com.iisquare.fs.base.core.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Test;

import java.util.*;

public class DataTester {

    @Test
    public void splitTest() {
        String text = ":::";
        System.out.println(Arrays.toString(text.split(":")));
        System.out.println(Arrays.toString(text.split(":", -1)));
    }

    @Test
    public void regexTest() {
        String regex = "[^\\u4E00-\\u9FFFa-zA-Z0-9]";
        String text = "关于下达2023年中央农业产业发展资金（畜牧部分脱贫县统筹整合部分）的通知（吉财农指【2023】319号）";
        System.out.println(text.replaceAll(regex, " "));
    }

    @Test
    public void numberTest() {
        List<String> list = Arrays.asList("a", "b", "c");
        JsonNode json = DPUtil.toJSON(list);
        Iterator<JsonNode> iterator = json.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            String text = DPUtil.value(item, "").asText();
            System.out.println(text);
        }
    }

    @Test
    public void arrayTest() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] strings = list.toArray(new String[0]);
        System.out.println(DPUtil.sublist(list, 1, 5));
        System.out.println(Arrays.toString(strings));
        System.out.println(Arrays.toString(DPUtil.array(String.class, 2)));
        JsonNode json = DPUtil.toJSON(list);
        System.out.println(json.get(6));
    }

    @Test
    public void jsonTest() {
        ObjectNode json = DPUtil.objectNode();
        ArrayNode array = json.putObject("a").putArray("b");
        array.addObject().put("c", 1);
        array.addObject().put("d", "xxx");
        array.addObject().putObject("e").put("f", 3);
        System.out.println(DPUtil.value(json, "a.b.2.e.f", DPUtil.toJSON(666)));
        JsonNode result = DPUtil.value(json, "a.b.2.e");
        System.out.println(DPUtil.stringify(result));
        Iterator<Map.Entry<String, JsonNode>> iterator = DPUtil.value(json, "a.b").fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            System.out.println(String.format("field %s type is %s", entry.getKey(), entry.getValue().getNodeType()));
        }
    }

    @Test
    public void typeTest() {
        ObjectNode json = DPUtil.objectNode();
        json.put("a", 1);
        json.put("b", true);
        json.put("c", "text");
        json.put("d", 2L);
        json.put("e", 3f);
        json.putObject("f");
        json.putArray("g");
        json.put("h", false);
        Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            Class<?> cls = DPUtil.toJSON(entry.getValue(), Object.class).getClass();
            System.out.println(String.format("%s -> %s : %s", entry.getKey(), cls, entry.getValue().asText()));
        }
    }

    @Test
    public void duplicateTest() {
        Set<Object> hs = new LinkedHashSet<>();
        hs.add(DPUtil.objectNode().put("id", 1).put("name", "a"));
        hs.add(DPUtil.objectNode().put("id", 2).put("name", "b"));
        hs.add(DPUtil.objectNode().put("id", 1).put("name", "a"));
        hs.add(DPUtil.objectNode().put("id", 3).put("name", "c"));
        hs.add(DPUtil.buildMap("id", 2, "name", "b"));
        hs.add(DPUtil.buildMap("id", 4, "name", "d"));
        hs.add(DPUtil.buildMap("id", 5, "name", "e"));
        hs.add(DPUtil.buildMap("id", 4, "name", "d"));
        System.out.println(hs);
    }

}
