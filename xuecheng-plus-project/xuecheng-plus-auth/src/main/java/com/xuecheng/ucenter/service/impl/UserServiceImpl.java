package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 *自定义UserDetailsService用来对接Spring Security
 */
@Slf4j
@Component
public class UserServiceImpl implements UserDetailsService {
    @Autowired
    private XcUserMapper xcUserMapper;
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 查询用户信息
     * @param s AuthParamsDto类型的json数据
     * @return
     * @throws UsernameNotFoundException
     */
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        AuthParamsDto authParamsDto=null;
        try {
            //将认证参数转为AuthParamsDto类型
            authParamsDto=JSON.parseObject(s,AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("认证请求数据格式不对");
        }

        //认证类型，password、wx...
        String authType = authParamsDto.getAuthType();
        //根据认证类型从spring容器中选择对应的bean
        AuthService authService = applicationContext.getBean(authType + "_authservice",AuthService.class);
        //调用统一认证execute方法
        XcUserExt xcUser = authService.execute(authParamsDto);

        //将查询到的用户信息封装为UserDetails返回给spring security框架,框架进行密码校验
        return getUserPrincipal(xcUser);
    }

    /**
     * 将结果封装为UserDetails
     * @param xcUser
     * @return
     */
    private UserDetails getUserPrincipal(XcUserExt xcUser){

        String password = xcUser.getPassword();
        //用户权限
        String[] authorities= {"test"};
        //处理敏感数据
        xcUser.setPassword(null);
        //将用户数据转为JSON
        String userJson = JSON.toJSONString(xcUser);
        UserDetails userDetails = User.withUsername(userJson).password(password).authorities(authorities).build();
        return userDetails;
    }
}
