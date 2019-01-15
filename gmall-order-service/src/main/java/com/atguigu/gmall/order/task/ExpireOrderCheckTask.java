package com.atguigu.gmall.order.task;


import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@EnableScheduling
public class ExpireOrderCheckTask {

    @Scheduled(cron = "0/10 * * * * ?")
    public void work() throws InterruptedException {
        System.out.println("开始扫描过期订单" + Thread.currentThread()+new Date());
    }

}
