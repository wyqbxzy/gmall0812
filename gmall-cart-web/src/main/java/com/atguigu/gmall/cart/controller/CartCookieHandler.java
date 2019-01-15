package com.atguigu.gmall.cart.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartCookieHandler {


    public void addToCart(HttpServletRequest request, HttpServletResponse response, CartInfo cartInfo) {
        List<CartInfo> cartInfos = new ArrayList<CartInfo>();
        // 判断是否空
        String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
        if(StringUtils.isBlank(cartListCookie)){
            // cookie中没有，添加cookie
            cartInfos.add(cartInfo);
        }else{
            //　判断是否有重复的购物车数据
            cartInfos = JSON.parseArray(cartListCookie, CartInfo.class);
            boolean b = if_new_cart(cartInfos,cartInfo);
            if(b){
                // 添加
                cartInfos.add(cartInfo);
           }else{
                // 更新，数量和购物车合计
                for (CartInfo info : cartInfos) {
                    if(info.getSkuId().equals(cartInfo.getSkuId())){
                        info.setSkuNum(info.getSkuNum()+cartInfo.getSkuNum());
                        info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));//bigDecimal
                    }
                }
           }
        }
        // 覆盖浏览器cookie
        CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(cartInfos),60*60*24*7,true);

    }

    private boolean if_new_cart(List<CartInfo> cartInfos, CartInfo cartInfo) {
        boolean b = true;
        for (CartInfo info : cartInfos) {
            if(info.getSkuId().equals(cartInfo.getSkuId())){
                b = false;
            }
        }
        return b;
    }
}
