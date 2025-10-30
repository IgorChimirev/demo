package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MainController {

    @GetMapping("/")
    public String index(@RequestParam(name = "tgWebAppData", required = false) String tgWebAppData,
                        Model model) {

        boolean isTelegram = tgWebAppData != null && !tgWebAppData.isEmpty();
        model.addAttribute("isTelegram", isTelegram);

        if (isTelegram) {
            model.addAttribute("telegramData", tgWebAppData);
        }

        return "index";
    }


    @GetMapping("/api/user")
    @ResponseBody
    public String getUserInfo() {
        return "Данные пользователя с бэкенда: ID 12345, Имя: Тестовый пользователь";
    }

}