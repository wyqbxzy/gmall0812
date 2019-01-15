package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayResponse;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    @Reference
    OrderService orderService;

    @Reference
    PaymentService paymentService;

    @RequestMapping("alipay/callback/return")
    public String alipayReturn(HttpServletRequest request, ModelMap map) {
        // request请求中包含的参数
        String queryString = request.getQueryString();

        // 从请求中取出所有参数
        String trade_no = request.getParameter("trade_no");
        String app_id = request.getParameter("app_id");
        String out_trade_no = request.getParameter("out_trade_no");
        String timestamp = request.getParameter("timestamp");
        String sign = request.getParameter("sign");
        String sign_type = request.getParameter("sign_type");
        String auth_app_id = request.getParameter("auth_app_id");
        String charset = request.getParameter("charset");
        String seller_id = request.getParameter("seller_id");
        String method = request.getParameter("method");
        String version = request.getParameter("version");
        // 模拟封装异步通知参数
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("trade_no", trade_no);
        paramsMap.put("app_id", app_id);
        paramsMap.put("out_trade_no", out_trade_no);
        paramsMap.put("timestamp", timestamp);
        paramsMap.put("sign", sign);
        paramsMap.put("sign_type", sign_type);
        paramsMap.put("auth_app_id", auth_app_id);
        paramsMap.put("charset", charset);
        paramsMap.put("seller_id", seller_id);
        paramsMap.put("method", method);
        paramsMap.put("version", version);


        // 验证支付宝的签名
        boolean signVerified = false;
        try {
            Map<String, String> checkMap = new HashMap<String, String>();
            checkMap.put("sign", sign);
            checkMap.put("sign_type", sign_type);
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
            System.out.println();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        signVerified = true;
        if (signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure

            if (1 == 1) {//trade_status.equals("TRADE_SUCCESS")||trade_status.equals("TRADE_FINISHED")
                // 更新交易信息
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setOutTradeNo(out_trade_no);
                paymentInfo.setCallbackTime(new Date());
                HashMap<String, Object> stringObjectHashMap = new HashMap<>();
                paymentInfo.setCallbackContent(queryString);

                paymentInfo.setAlipayTradeNo(trade_no);
                paymentService.updatePayment(paymentInfo);
            } else {
                // 该笔交易存在问题
            }

        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.

        }


        return "finish";
    }

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("index")
    public String index(String orderId, String totalAmount, ModelMap map) {

        map.put("orderId", orderId);
        map.put("totalAmount", totalAmount);
        return "paymentindex";
    }

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("mx/submit")
    public String mx(String orderId, ModelMap map) {

        return "";
    }

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("alipay/submit")
    @ResponseBody
    public String alipay(String orderId, ModelMap map) {

        // 根据订单id查询订单数据
        OrderInfo orderInfo = new OrderInfo();
        orderInfo = orderService.getOrderById(orderId);

        // 根据订单数据封装支付宝表单
        //out_trade_no
        //product_code
        //total_amount
        //subject
        //body
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        // 需要在表单参数中设置支付宝回跳的地址和支付宝通知支付服务接口的地址
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        Map<String, Object> requestMap = new HashMap<String, Object>();

        requestMap.put("out_trade_no", orderInfo.getOutTradeNo());
        requestMap.put("product_code", "FAST_INSTANT_TRADE_PAY");
        requestMap.put("total_amount", 0.01);//orderInfo.getTotalAmount()
        requestMap.put("subject", orderInfo.getOrderDetailList().get(0).getSkuName());
        requestMap.put("body", orderInfo.getOrderComment());

        String s = JSON.toJSONString(requestMap);

        alipayRequest.setBizContent(s);//填充业务参数
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        // 保存交易信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.toString());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getOrderDetailList().get(0).getSkuName());
        paymentInfo.setOrderId(orderId);
        paymentInfo.setCreateTime(new Date());

        paymentService.savePayment(paymentInfo);



        // 为了确保支付行为被成功通知，在提交支付之前，设置一个支付服务对支付平台结果的延迟检查
        System.out.println("提交订单，发送支付状态检查队列，设置检查次数为5次");
        paymentService.sendCheckQueue(paymentInfo.getOutTradeNo(),5);

        return form;
    }

}
