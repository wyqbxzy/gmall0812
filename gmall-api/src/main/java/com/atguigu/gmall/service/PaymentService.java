package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {
    void updatePayment(PaymentInfo paymentInfo);

    void savePayment(PaymentInfo paymentInfo);

    void sendCheckQueue(String outTradeNo, int i);

    void checkPaymentResult(String outTradeNo, int i);
}
