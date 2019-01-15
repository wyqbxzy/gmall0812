package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    @RequestMapping("index")
    public String index(String returnUrl, ModelMap map) {
        map.put("returnUrl", returnUrl);
        return "index";
    }


    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request, HttpServletResponse response, UserInfo userInfo) {

        // 验证用户
        UserInfo login = userService.login(userInfo);

        if (login == null) {
            // 用户名或者密码错误
            return "fail";
        } else {
            // 生成token
            HashMap<String, String> userMap = new HashMap<>();
            userMap.put("userId", login.getId());
            userMap.put("nickName", login.getNickName());
            // 获取登陆客户端的ip
            String ip = getIp(request);
            String token = JwtUtil.encode("gmall0115tokenkey", userMap, ip);

            // 用户登录成功，将未登录和登陆的购物车数据进行合并
            // cartListCookie cartListDb
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            cartService.mergeCart(cartListCookie,login.getId());
            // 清空cookie购物车
            CookieUtil.setCookie(request,response,"cartListCookie","",0,false);

            // 返回token
            return token;
        }
    }

    /***
     * 获得客户端ip
     * @param request
     * @return
     */
    private String getIp(HttpServletRequest request) {
        String ip = "";
        // 经过nginx负载均衡的反向代理
        ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }

        if (StringUtils.isBlank(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * *校验证书真伪
     * @param token
     * @param ip
     * @return
     */
    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token,String ip) {
        // 校验token
        Map userMap = JwtUtil.decode("gmall0115tokenkey", token, ip);

        // 验证redis中的过期时间，刷新过期时间
        if(userMap!=null){
            String userId = userMap.get("userId").toString();
            UserInfo verify = userService.verify(userId);
            if(verify==null){
                return "fail";
            }else{
                return "success";//fail
            }
        }else{
            return "fail";

        }

    }
}
