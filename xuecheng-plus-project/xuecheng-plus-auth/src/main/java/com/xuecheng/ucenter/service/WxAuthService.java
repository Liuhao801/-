package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * 微信认证接口
 */
public interface WxAuthService {
    /**
     * 收到code调用微信接口申请access_token，获取用户信息，添加用户到数据库
     * @param code 授权码
     * @return
     */
    public XcUser wxAuth(String code);
}
