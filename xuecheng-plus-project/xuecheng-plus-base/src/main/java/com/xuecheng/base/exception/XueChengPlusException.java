package com.xuecheng.base.exception;

/**
 * 自定义异常类型
 */
public class XueChengPlusException extends RuntimeException{
    private String errMessage;

    public XueChengPlusException() {
        super();
    }

    public XueChengPlusException(String errMessage) {
        super(errMessage);
        this.errMessage=errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    //抛出自定义异常信息
    public static void cast(String errMessage){
        throw new XueChengPlusException(errMessage);
    }
    //抛出自定义异常信息
    public static void cast(CommonError commonError){
        throw new XueChengPlusException(commonError.getErrMessage());
    }
}
