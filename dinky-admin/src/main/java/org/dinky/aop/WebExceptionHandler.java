/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dinky.aop;

import org.dinky.common.result.Result;
import org.dinky.exception.BusException;
import org.dinky.model.CodeEnum;
import org.dinky.utils.MessageResolverUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.core.util.StrUtil;

/**
 * WebExceptionHandler
 *
 * @author wenmo
 * @since 2022/2/2 22:22
 */
@ControllerAdvice
@ResponseBody
public class WebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler
    public Result<Void> busException(BusException e) {
        if (StrUtil.isNotEmpty(e.getMsg())) {
            return Result.failed(MessageResolverUtils.getMessages(e.getCode(), e.getErrorArgs()));
        }
        return Result.failed(e.getMsg());
    }

    @ExceptionHandler
    public Result<Void> notLoginException(NotLoginException e) {
        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = servletRequestAttributes.getResponse();
        response.setStatus(CodeEnum.NOTLOGIN.getCode());
        return Result.notLogin(MessageResolverUtils.getMessage("login.not.login"));
    }

    /**
     * 参数异常处理程序 设置状态码为 400
     *
     * @param e e
     * @return {@link Result}<{@link String}>
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public Result<String> paramExceptionHandler(MethodArgumentNotValidException e) {
        BindingResult exceptions = e.getBindingResult();
        // 判断异常中是否有错误信息，如果存在就使用异常中的消息，否则使用默认消息
        if (exceptions.hasErrors()) {
            List<ObjectError> errors = exceptions.getAllErrors();
            if (!errors.isEmpty()) {
                // 这里列出了全部错误参数，按正常逻辑，只需要第一条错误即可
                FieldError fieldError = (FieldError) errors.get(0);
                if (StringUtils.isNotBlank(fieldError.getDefaultMessage())) {
                    return Result.failed(
                            String.format(
                                    "字段:%s, %s",
                                    fieldError.getField(), fieldError.getDefaultMessage()));
                }
                return Result.failed(
                        String.format(
                                "字段:%s,不合法的值:%s",
                                fieldError.getField(), fieldError.getRejectedValue()));
            }
        }
        return Result.failed(MessageResolverUtils.getMessage("request.params.error"));
    }

    @ExceptionHandler
    public Result<Void> unknownException(Exception e) {
        logger.error("ERROR:", e);
        return Result.failed(e.getMessage());
    }
}
