package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

public interface OrderService {
    boolean checkTradeCode(String userId, String tradeCode);

    OrderInfo saveOrder(OrderInfo orderInfo);

    String genTradeCode(String userId);

    OrderInfo getOrderById(String orderId);

    void updateOrderStatus(String outTradeNo, String tradeStatus);

    void sendOrderPaidQueue(String outTradeNo);
}
