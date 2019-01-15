package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    PaymentInfoMapper paymentInfoMapper;


    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePayment(PaymentInfo paymentInfo) {

        paymentInfoMapper.insertSelective(paymentInfo);

    }

    /**
     * 付款后更新支付状态
     *
     * @param paymentInfo
     */
    @Override
    public void updatePayment(PaymentInfo paymentInfo) {

        Example e = new Example(PaymentInfo.class);// update table set ... where c1 = ? and c2 = ?
        e.createCriteria().andEqualTo(paymentInfo.getOutTradeNo());

        // 发送支付消息，给订单系统消费
        sendPaymentResult(paymentInfo);

        paymentInfoMapper.updateByExampleSelective(paymentInfo, e);


    }


    /***
     * 支付提交后设置，发出延迟检查的消息
     */
    @Override
    public void checkPaymentResult(String outTradeNo, int counter) {
        boolean b = checkPayMentStatusByOutTradeNo(outTradeNo);
        if (counter > 0&&b) {
            // 查询支付宝交易状态接口
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{" +
                    "\"out_trade_no\":\"" + outTradeNo + "\"}");
            AlipayTradeQueryResponse response = null;
            try {
                response = alipayClient.execute(request);
            } catch (AlipayApiException e) {
            }

            if (response.isSuccess()) {
                // 停止延迟检查，发送支付成功结果的队列
                String tradeStatus = response.getTradeStatus();
                // 如果手机未扫码，此时状态为null
                if (StringUtils.isNotBlank(tradeStatus) && (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED"))) {
                    // 更新支付状态，发送支付成功的消息队列
                    // 更新交易信息
                    PaymentInfo paymentInfo = new PaymentInfo();
                    paymentInfo.setPaymentStatus("已支付");
                    paymentInfo.setOutTradeNo(response.getOutTradeNo());
                    paymentInfo.setCallbackTime(new Date());
                    HashMap<String, Object> stringObjectHashMap = new HashMap<>();
                    paymentInfo.setCallbackContent(response.getMsg());

                    paymentInfo.setAlipayTradeNo(response.getTradeNo());
                    updatePayment(paymentInfo);

                    sendPaymentResult(paymentInfo);
                    System.out.println("已经检查完毕或者检查到结果，循检结束，发送已支付的消息。。。");

                    return;
                }

            }

            // 如果返回的调用结果是支付未完成,继续发送检查的消息队列
            System.out.println("交易未结束并且检查次数大于0,"+(5-counter)+"次检查结束，继续循检。。。");
            sendCheckQueue(outTradeNo,counter);
        }
    }

    /***
     * 1 发送检查队列
     * 2 第一次提交订单时，发送一个检查订单的队列
     * 3 之后再检查订单，调用支付宝的支付时，根据支付情况决定是否调用该队列
     */
    @Override
    public void sendCheckQueue(String outTradeNo, int counter){
        Connection connection = activeMQUtil.getConnection();
        Session session = null;
        try {
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_success_queue = session.createQueue("PAYMENT_CHECK_QUEUE");

            MessageProducer producer = session.createProducer(payment_success_queue);
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo", outTradeNo);
            mapMessage.setInt("counter", counter);
            // 设置延迟检查消息的时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 1000 * 60);
            producer.send(mapMessage);

            session.commit();
            session.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * 幂等性检查
     *
     * @param outTradeNo
     * @return
     */
    private boolean checkPayMentStatusByOutTradeNo(String outTradeNo) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo = paymentInfoMapper.selectOne(paymentInfo);

        if(paymentInfo.getPaymentStatus().equals("已支付")){
            return false;
        }else{
            return true;
        }

    }

    /***
     * 被成功通知后，发出支付结果的消息
     * @param paymentInfo
     */
    private void sendPaymentResult(PaymentInfo paymentInfo) {

        Connection connection = activeMQUtil.getConnection();

        Session session = null;

        try {
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_success_queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");

            MessageProducer producer = session.createProducer(payment_success_queue);
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo", paymentInfo.getOutTradeNo());
            mapMessage.setString("tradeStatus", paymentInfo.getPaymentStatus());

            producer.send(mapMessage);

            session.commit();
            session.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
