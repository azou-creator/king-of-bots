package com.kob.backend.common;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理：所有异常转换为 R 结构（HTTP 200 + 业务 status），
 * 避免异常穿透到 Security 过滤器变成 403/500 空 body，导致前端拿不到错误信息。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Log log = LogFactory.get();

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public R<String> handleBadCredentials(Exception e) {
        // 不区分"用户不存在"和"密码错误"，避免泄露账号是否存在
        return R.fail("用户名或密码错误");
    }

    @ExceptionHandler(AuthenticationException.class)
    public R<String> handleAuthentication(AuthenticationException e) {
        return R.fail("登录失败：" + e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<String> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        return R.fail(fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<String> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<String> handleUnreadable(HttpMessageNotReadableException e) {
        return R.fail("请求体格式错误");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public R<String> handleDataIntegrity(DataIntegrityViolationException e) {
        return R.fail("数据保存失败，内容可能已存在");
    }

    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        log.error(e);
        return R.fail("服务器内部错误，请稍后重试");
    }
}
