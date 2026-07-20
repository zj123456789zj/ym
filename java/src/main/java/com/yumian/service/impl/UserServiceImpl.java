package com.yumian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yumian.common.exception.BusinessException;
import com.yumian.entity.User;
import com.yumian.mapper.UserMapper;
import com.yumian.service.UserService;
import com.yumian.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    @Override
    public void register(String username, String password, String nickname, String captchaCode) {
        // 校验验证码
        if (captchaCode == null || captchaCode.trim().isEmpty()) {
            throw new BusinessException(410, "请填写验证码");
        }
        String storedCode = redisTemplate.opsForValue().get("captcha_code");
        if (storedCode == null) {
            throw new BusinessException(410, "验证码已过期，请重新获取");
        }
        if (!storedCode.equals(captchaCode)) {
            throw new BusinessException(410, "验证码错误");
        }
        // 删除已使用的验证码（一次性）
        redisTemplate.delete("captcha_code");

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (count > 0) {
            throw new BusinessException(410, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        userMapper.insert(user);
    }

    @Override
    public User getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(Long userId, User user) {
        user.setId(userId);
        userMapper.updateById(user);
    }
}
