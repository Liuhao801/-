package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.apache.commons.lang.StringUtils;

/**
 * 账号密码认证
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {
    @Autowired
    private XcUserMapper xcUserMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CheckCodeClient checkCodeClient;

    /**
     * 密码认证
     * @param authParamsDto 认证参数
     * @return
     */
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //账号
        String username = authParamsDto.getUsername();

        //远程调用验证码服务接口校验验证码
        //输入的验证码
        String checkcode = authParamsDto.getCheckcode();
        //验证码在redis中的key
        String checkcodekey = authParamsDto.getCheckcodekey();
        if(StringUtils.isBlank(checkcode)||StringUtils.isBlank(checkcodekey)){
            throw new RuntimeException("验证码为空");
        }
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if(!verify){
            throw new RuntimeException("验证码输入错误");
        }

        //从数据库中查询用户
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        //用户不存在
        if(xcUser==null){
            throw new RuntimeException("账号不存在");
        }

        XcUserExt xcUserExt=new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        //数据库加密后密码
        String passwordDb= xcUser.getPassword();
        //认证参数中的密码
        String passwordFrom = authParamsDto.getPassword();
        //校验密码
        boolean matches = passwordEncoder.matches(passwordFrom, passwordDb);
        if(!matches){
            throw new RuntimeException("用户名或密码错误");
        }
        return xcUserExt;
    }
}
