package com.atguigu.gmall.payment.mqTest;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class YuTingT {

    public static void main(String[] args) throws Exception {

        // 1 mq连接对象工厂
        ConnectionFactory connect = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER, ActiveMQConnection.DEFAULT_PASSWORD, "tcp://localhost:61616");
        // 2 得到一个mq的连接
        Connection connection = connect.createConnection();
        connection.setClientID("2");
        connection.start();

        // 3 创建一次mq的任务(会话)
        //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic("ganggeslogan");

        // 4 创建一个mqganggeheshui监听对象

        MessageConsumer consumer = session.createDurableSubscriber(topic,"2");
        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if (message instanceof TextMessage) {
                    try {
                        String text = ((TextMessage) message).getText();
                        System.out.println("李玉婷老师,听到："+text+"本月工资不领了...");

                        //session.rollback();
                    } catch (JMSException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });


    }
}
