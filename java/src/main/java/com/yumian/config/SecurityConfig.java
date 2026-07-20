package com.yumian.config;

import com.yumian.entity.User;
import com.yumian.mapper.UserMapper;
import com.yumian.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/login", "/api/user/register", "/api/captcha/**").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String path = request.getRequestURI();
                boolean isPublic = path.equals("/api/user/login") || path.equals("/api/user/register")
                        || path.startsWith("/api/captcha/");

                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
                    try {
                        String token = authHeader.substring(7);
                        Long userId = jwtUtil.getUserIdFromToken(token);
                        if (userId != null) {
                            request.setAttribute("userId", userId);
                            User user = userMapper.selectById(userId);
                            request.setAttribute("role", user != null && user.getRole() != null ? user.getRole() : "user");
                        } else if (!isPublic) {
                            writeUnauthorized(response);
                            return;
                        }
                    } catch (Exception e) {
                        log.warn("无效的 JWT token: {}", e.getMessage());
                        if (!isPublic) {
                            writeUnauthorized(response);
                            return;
                        }
                    }
                } else if (!isPublic) {
                    writeUnauthorized(response);
                    return;
                }
                filterChain.doFilter(request, response);
            }

            private void writeUnauthorized(HttpServletResponse response) throws IOException {
                response.setStatus(401);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
