package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.TextMessage;

@Component
public class OrderMqListener {

    @Autowired
    OrderService orderService;

    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage){

        try {
            String outTradeNo =   mapMessage.getString("outTradeNo");
            String tradeStatus =  mapMessage.getString("tradeStatus");

            // 根据支付消息，更新订单状态
            orderService.updateOrderStatus(outTradeNo,tradeStatus);

            // 通知库存系统，启动库存服务
            orderService.sendOrderPaidQueue(outTradeNo);

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
