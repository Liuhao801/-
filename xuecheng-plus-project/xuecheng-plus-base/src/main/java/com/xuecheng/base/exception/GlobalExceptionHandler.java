package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理器
 */
@Slf4j
@ControllerAdvice
//@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义异常
     * @param e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(XueChengPlusException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(XueChengPlusException e){
        log.error("系统异常：{}",e.getErrMessage(),e);
        return new RestErrorResponse(e.getErrMessage());
    }

    /**
     * 处理系统异常
     * @param e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(Exception e){
        log.error("系统异常：{}",e.getMessage(),e);
        if(e.getMessage().equals("Access is denied")){
            return new RestErrorResponse("没有操作此功能的权限");
        }
        return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    }

    /**
     * 处理表单参数异常
     * @param e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(MethodArgumentNotValidException e){
        BindingResult bindingResult = e.getBindingResult();
        List<String> msgList = new ArrayList<>();
        //将错误信息放在msgList
        bindingResult.getFieldErrors().stream().forEach(item->msgList.add(item.getDefaultMessage()));
        //拼接错误信息
        String msg = StringUtils.join(msgList, ",");
        log.error("系统异常{}",msg);
        return new RestErrorResponse(msg);
    }
}
