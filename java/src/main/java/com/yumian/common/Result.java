package com.yumian.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    private static final int CODE_SUCCESS = 200;
    private static final int CODE_BAD_REQUEST = 400;
    private static final int CODE_UNAUTHORIZED = 401;
    private static final int CODE_SERVER_ERROR = 500;

    public static <T> Result<T> success(T data) {
        return new Result<>(CODE_SUCCESS, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(CODE_SUCCESS, "success", null);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(CODE_SUCCESS, message, data);
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(CODE_BAD_REQUEST, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(CODE_UNAUTHORIZED, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(CODE_SERVER_ERROR, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
