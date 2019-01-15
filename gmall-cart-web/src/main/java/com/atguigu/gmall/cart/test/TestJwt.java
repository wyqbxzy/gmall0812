package com.atguigu.gmall.cart.test;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.JwtUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class TestJwt {

    public static void main(String[] args){

        HashMap<String, String> userMap = new HashMap<>();
        userMap.put("userId","2");
        userMap.put("nickName","jerry");
        String ip = "127.0.0.1";
        String token = JwtUtil.encode("gmall0115tokenkey", userMap, ip);

        System.out.println(JSON.toJSONString(token));
        Map token1 = JwtUtil.decode("gmall0115tokenkey", token, ip);
        System.out.println(JSON.toJSONString(token1));

        Base64UrlCodec b = new Base64UrlCodec();

        String s = StringUtils.substringBetween(token, ".");
        
        System.out.println(s);

        byte[] decode = b.decode(s);

        String str = new String(decode);

        System.out.println(str);

    }
}
