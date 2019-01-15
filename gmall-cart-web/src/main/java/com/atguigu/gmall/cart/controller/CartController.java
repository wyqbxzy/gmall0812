package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    CartService cartService;

    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("checkCart")
    public String checkCart(HttpServletResponse response,HttpServletRequest request,String skuId,String isCheckedFlag, ModelMap map){
        List<CartInfo> cartInfos = new ArrayList<>();
        request.getAttribute("userId");
        String userId = (String)request.getAttribute("userId");//"2";//"1";
        // 根据用户是否登陆，修改选中状态
        if(StringUtils.isBlank(userId)){
            // 取出cookie中的购物车集合
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            cartInfos = JSON.parseArray(cartListCookie, CartInfo.class);
            // 对选中状态进行操作
            for (CartInfo cartInfo : cartInfos) {
                if(cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setIsChecked(isCheckedFlag);
                }
            }
            CookieUtil.setCookie(request,response,"cartListCookie",JSON.toJSONString(cartInfos),60*60*24*7,true);

        }else{
            // 操作db
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setIsChecked(isCheckedFlag);
            cartService.checkCart(cartInfo);

            cartInfos = cartService.cartList(userId);
        }

        if(cartInfos!=null){
            map.put("totalPrice",getTotlePrice(cartInfos));
        }
        map.put("cartInfos",cartInfos);
        return "cartListInner";
    }

    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request, ModelMap map){
        List<CartInfo> cartInfos = new ArrayList<>();
        String userId = (String)request.getAttribute("userId");//"2";//"1";
        if(StringUtils.isBlank(userId)){
            // 取出cookie中的购物车集合
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            cartInfos = JSON.parseArray(cartListCookie, CartInfo.class);
        }else{
            // 出去缓存中的购物车集合
            cartInfos= cartService.cartList(userId);
        }

        if(cartInfos!=null){
            map.put("totalPrice",getTotlePrice(cartInfos));
        }
        map.put("cartInfos",cartInfos);
        return "cartList";
    }

    private BigDecimal getTotlePrice(List<CartInfo> cartInfos) {

        BigDecimal totalPrice = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfos) {
            if(cartInfo.getIsChecked().equals("1")){
                BigDecimal cartPrice = cartInfo.getCartPrice();
                totalPrice = totalPrice.add(cartPrice);
            }
        }
        return totalPrice;
    }

    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("addToCart")
    public ModelAndView addToCart(HttpServletRequest request, HttpServletResponse response, CartInfo cartInfo, ModelMap map){
        String userId = (String)request.getAttribute("userId");//"2";//"1";
        // 根据skuId查询购物车对象参数
        cartInfo.setIsChecked("1");
        cartInfo.setCartPrice(cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));

        // 判断用户是否登陆
        if(StringUtils.isBlank(userId)){
            // 未登录，操作cookie
            CartCookieHandler cartCookieHandler = new CartCookieHandler();
            cartCookieHandler.addToCart(request,response,cartInfo);

        }else{
            // 已登陆，操作db
            cartInfo.setUserId(userId);
            cartService.addToCart(cartInfo);
        }

        ModelAndView mv = new ModelAndView("redirect:/cartSuccess");
        mv.addObject("skuDefaultImg",cartInfo.getImgUrl());
        mv.addObject("id",cartInfo.getSkuId());
        mv.addObject("skuName",cartInfo.getSkuName());
        mv.addObject("skuNum",cartInfo.getSkuNum());

        return mv;
    }

    @RequestMapping("cartSuccess")
    public String cartSuccess(SkuInfo skuInfo,String skuNum,ModelMap map){
        map.put("skuInfo",skuInfo);
        map.put("skuNum",skuNum);

        return "success";
    }
}
