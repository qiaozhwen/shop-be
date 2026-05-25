package com.qzshop.shopbe.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Day 1 - 你的第一个 Spring Boot 接口
 *
 * TODO 练习：
 * 1. 启动项目后访问 http://localhost:8080/hello 看看返回什么
 * 2. 访问 http://localhost:8080/hello?name=你的名字 试试
 * 3. 试着自己添加一个新的接口，比如 /time 返回当前时间
 */
@RestController
public class HelloController {

    @GetMapping("/hello")
    public Map<String, Object> hello(@RequestParam(defaultValue = "World") String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hello, " + name + "!");
        result.put("status", "success");
        return result;
    }
}
