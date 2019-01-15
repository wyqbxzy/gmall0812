package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    List<CartInfo> checkCart(CartInfo cartInfo);

    List<CartInfo> cartList(String userId);

    void addToCart(CartInfo cartInfo);

    void mergeCart(String cartListCookie, String id);

    List<CartInfo> cartListByChecked(String userId);

    void deleteCartInfos(List<CartInfo> cartInfos);
}
