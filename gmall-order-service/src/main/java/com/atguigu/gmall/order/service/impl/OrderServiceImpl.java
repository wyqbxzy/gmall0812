package com.atguigu.gmall.order.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OrderInfoMapper orderInfoMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;


    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public boolean checkTradeCode(String userId, String tradeCode) {

        boolean b = false;
        String codeKey = "order:"+userId+":tradeCode";

        Jedis jedis = redisUtil.getJedis();
        String codeValue = jedis.get(codeKey);


        if(tradeCode.equals(codeValue)){
            b = true;

            // 销毁tradeCode

            jedis.del(codeKey);

        }
        jedis.close();
        return b;
    }

    @Override
    public String genTradeCode(String userId) {

        String codeKey = "order:"+userId+":tradeCode";
        String codeValue = UUID.randomUUID().toString();

        Jedis jedis = redisUtil.getJedis();
        jedis.setex(codeKey,60*10,codeValue);

        jedis.close();

        return codeValue;
    }

    @Override
    public OrderInfo saveOrder(OrderInfo orderInfo) {
        // 保存订单，生成主键
        orderInfoMapper.insertSelective(orderInfo);

        // 根据主键，保存订单详情
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }

        return orderInfo;
    }

    @Override
    public OrderInfo getOrderById(String orderId) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> select = orderDetailMapper.select(orderDetail);

        OrderInfo orderInfo1 = orderInfoMapper.selectByPrimaryKey(orderInfo);
        orderInfo1.setOrderDetailList(select);
        return orderInfo1;
    }

    @Override
    public void updateOrderStatus(String outTradeNo, String tradeStatus) {
        Example e = new Example(OrderInfo.class);
        e.createCriteria().andEqualTo("outTradeNo",outTradeNo);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setOrderStatus(tradeStatus);
        orderInfoMapper.updateByExample(orderInfo,e);
    }

    @Override
    public void sendOrderPaidQueue(String outTradeNo) {

        // 根据outTradeNo取出订单对象
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(outTradeNo);
        OrderInfo selectOrderInfo = orderInfoMapper.selectOne(orderInfo);
        String orderId = selectOrderInfo.getId();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> selectOrderDetail = orderDetailMapper.select(orderDetail);
        selectOrderInfo.setOrderDetailList(selectOrderDetail);

        // 将取出的订单对象封装成mq的message
        String s = JSON.toJSONString(selectOrderInfo);

        // 发出order_paid_queue
        // 如果返回的调用结果是支付未完成,继续发送检查的消息队列
        Connection connection = activeMQUtil.getConnection();
        Session session = null;
        try {
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_paid_queue = session.createQueue("ORDER_PAID_QUEUE");

            MessageProducer producer = session.createProducer(order_paid_queue);
            TextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(s);
            // 设置延迟检查消息的时间
            producer.send(textMessage);

            session.commit();
            session.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }



    }
}
