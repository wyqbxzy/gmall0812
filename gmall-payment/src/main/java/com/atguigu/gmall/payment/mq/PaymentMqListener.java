package com.atguigu.gmall.payment.mq;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentMqListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage){

        String outTradeNo = null;
        int counter = 0;
        try {
            outTradeNo = mapMessage.getString("outTradeNo");
            counter = mapMessage.getInt("counter");
        } catch (JMSException e) {
        }
        // 检查支付状态的延迟队列
        System.out.println("开始循环检查支付状态，开始第"+(6-counter)+"次。。。");
        paymentService.checkPaymentResult(outTradeNo,counter-1);

    }
}
