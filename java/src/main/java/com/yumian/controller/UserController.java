package com.yumian.controller;

import com.yumian.common.Result;
import com.yumian.dto.LoginRequest;
import com.yumian.dto.RegisterRequest;
import com.yumian.entity.User;
import com.yumian.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getNickname(),
                request.getCaptchaCode()
        );
        return Result.success();
    }

    @GetMapping("/profile")
    public Result<User> getProfile(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestAttribute("userId") Long userId,
                                      @RequestBody User user) {
        userService.updateProfile(userId, user);
        return Result.success();
    }
}
