package com.atguigu.gmall.cart.test;

import ch.qos.logback.core.net.SyslogOutputStream;

import java.math.BigDecimal;

public class TestBd {

    public static void main(String[] args){

        // 1 初始化问题
        BigDecimal b1 = new BigDecimal(0.01d);
        BigDecimal b2 = new BigDecimal(0.01f);
        System.out.println(b1);
        System.out.println(b2);
        BigDecimal b3 = new BigDecimal("0.01");
        System.out.println(b3);

        // 2 比较
        int i = b1.compareTo(b2);// -1 0 1
        System.out.println(i);

        // 3 运算
        BigDecimal b4 = new BigDecimal("6");
        BigDecimal b5 = new BigDecimal("7");

        BigDecimal add = b4.add(b5);
        System.out.println(add);
        BigDecimal subtract = b4.subtract(b5);
        System.out.println(subtract);
        BigDecimal multiply = b4.multiply(b5);
        System.out.println(multiply);

        // 4 运算约数
        BigDecimal divide = b4.divide(b5,3,BigDecimal.ROUND_HALF_DOWN);
        System.out.println(divide);

        BigDecimal add1 = b1.add(b2);
        System.out.println(add1);
        BigDecimal bigDecimal = add1.setScale(3, BigDecimal.ROUND_HALF_DOWN);
        System.out.println(bigDecimal);

    }

}
