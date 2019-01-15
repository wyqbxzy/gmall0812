package com.atguigu.gmall.payment.mqTest;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class JingBo {

    public static void main(String[] args) throws Exception {

        // 1 mq连接对象工厂
        ConnectionFactory connect = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER, ActiveMQConnection.DEFAULT_PASSWORD, "tcp://localhost:61616");
        // 2 得到一个mq的连接
        Connection connection = connect.createConnection();
        connection.start();

        // 3 创建一次mq的任务(会话)
        //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination testqueue = session.createQueue("ganggeheshui");

        // 4 创建一个mqganggeheshui监听对象
        MessageConsumer consumer = session.createConsumer(testqueue);
        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if (message instanceof TextMessage) {
                    try {
                        String text = ((TextMessage) message).getText();
                        System.out.println("靖博老师,听到："+text);

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
