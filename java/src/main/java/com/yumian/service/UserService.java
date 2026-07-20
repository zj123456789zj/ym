package com.yumian.service;

import com.yumian.entity.User;

import java.util.Map;

public interface UserService {

    Map<String, Object> login(String username, String password);

    void register(String username, String password, String nickname, String captchaCode);

    User getProfile(Long userId);

    void updateProfile(Long userId, User user);
}
