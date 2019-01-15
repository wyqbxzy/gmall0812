package com.atguigu.gmall.payment.mqTest;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class GangGe {

    public static void main(String[] args) throws  Exception{
            // 1 mq连接对象工厂
            ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://localhost:61616");

            // 2 得到一个mq的连接
            Connection connection = connect.createConnection();
            connection.start();

            // 3 创建一次mq的任务(会话)
            // 第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue testqueue = session.createQueue("ganggeheshui");

            // 4 执行mq的会话任务
            MessageProducer producer = session.createProducer(testqueue);
            TextMessage textMessage=new ActiveMQTextMessage();// 消息
            textMessage.setText("刚哥说我渴了。。。");//消息
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);//设置消息的持久化
            producer.send(textMessage);//发布消息
            session.commit();// 提交消息事务
            connection.close();//关闭连接


    }

}
