package com.atguigu.gmall.interceptor;

import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("web拦截器。。。");

        // 判断拦截的方法的类型
        HandlerMethod h = (HandlerMethod) handler;
        LoginRequire loginRequire = h.getMethodAnnotation(LoginRequire.class);

        if (loginRequire == null) {
            //不需要验证
            return true;
        }

        //需要验证
        boolean neededSuccess = loginRequire.isNeededSuccess();

        // 先获得token
        // 1 oldToken无值 newToken无值 从未登陆(必须登陆/可以不登陆)
        // 2 oldToken无值 newToken有值 第一次登录
        // 3 oldToken有值 newToken无值 之前登陆过
        // 4 oldToken有值 newToken有值 之前登陆过但是已经过期，或者证书伪造
        String oldToken = CookieUtil.getCookieValue(request, "userToken", true);
        String newToken = request.getParameter("token");
        String token = "";

        if (StringUtils.isBlank(oldToken) && StringUtils.isBlank(newToken) && neededSuccess) {
            // 之前从未登陆，但是必须登陆时，重定向到认证中心登陆
            StringBuffer requestURL = request.getRequestURL();
            response.sendRedirect("//passport.gmall.com:8085/index?returnUrl=" + requestURL);
            return false;
        }else if(StringUtils.isBlank(oldToken) && StringUtils.isBlank(newToken) && !neededSuccess){
            // 之前没有登陆过，但是不需要登陆
            return true;
        }else if ((StringUtils.isBlank(oldToken) && StringUtils.isNotBlank(newToken)) || (StringUtils.isNotBlank(oldToken) && StringUtils.isNotBlank(newToken))) {
            // 第一次登陆，验证newToken的正确性，将token写入cookie
            token = newToken;
        } else if(StringUtils.isNotBlank(oldToken) && StringUtils.isBlank(newToken)){
            // 之前登陆过，验证oldToken正确性，根据成功或者失败决定重新登陆或者通过
            token = oldToken;
        }

        // 调用认证中心，返回认证结果
        String verifyUrl = "http://passport.gmall.com:8085/verify?token="+token+"&ip="+getIp(request);
        String success = HttpClientUtil.doGet(verifyUrl);


        if(success.equals("fail")&&neededSuccess){
            // 认证失败打回认证中心重新登陆
            StringBuffer requestURL = request.getRequestURL();
            response.sendRedirect("//passport.gmall.com:8085/index?returnUrl=" + requestURL);
        }else if(success.equals("fail")&&!neededSuccess){

        }else if(success.equals("success")){
            if(token.equals(newToken)){
                // 第一次登陆，将token写入cookie
                CookieUtil.setCookie(request, response, "userToken", newToken, 60 * 30, true);
            }

            // 验证通过，需要将用户id和用户昵称写入到
            Map userMap = JwtUtil.decode("gmall0115tokenkey", token, getIp(request));
            String userId = userMap.get("userId").toString();
            String nickName = userMap.get("nickName").toString();
            request.setAttribute("userId",userId);
            request.setAttribute("nickName",nickName);
        }

        return true;
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


}
