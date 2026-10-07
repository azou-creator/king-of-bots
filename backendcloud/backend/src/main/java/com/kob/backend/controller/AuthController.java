package com.kob.backend.controller;

import com.kob.backend.common.R;
import com.kob.backend.common.SecurityUtils;
import com.kob.backend.dto.AddValid;
import com.kob.backend.dto.LoginValid;
import com.kob.backend.dto.UserParam;
import com.kob.backend.entity.User;
import com.kob.backend.service.serviceImpl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserServiceImpl userService;

    @PostMapping("/account/token")
    public R<Map<String, String>> login(@Validated(LoginValid.class) @RequestBody UserParam userParam) {

        Map<String, String> login = userService.login(userParam);
        return R.ok(login);
    }

    @PostMapping("/account/register")
    public R<String> register(@Validated(AddValid.class) @RequestBody UserParam userParam) {
        Map<String, String> register = userService.register(userParam);
        return R.ok(register.get("msg"));
    }


    @GetMapping("/account/{id}")
    public R<User> getUserInfo(@PathVariable("id") Long id) {
        User userInfo = userService.info(id);
        return R.ok(userInfo);
    }

    /** 上传/更换当前登录用户的头像, 返回新的头像 URL */
    @PostMapping("/avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        User user = SecurityUtils.getUser();
        return R.ok(userService.updateAvatar(user.getId(), file));
    }


}
