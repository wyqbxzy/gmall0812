package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    @Reference
    OrderService orderService;

    @Reference
    SkuService skuService;

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("submitOrder")
    public String submitOrder(HttpServletRequest request,String tradeCode,String deliveryAddress, ModelMap map){
        String userId = (String)request.getAttribute("userId");
        UserAddress userAddress = userService.getUserAddressByAddressId(deliveryAddress);


        // 检验交易码(tradeCode)
        boolean b = orderService.checkTradeCode(userId,tradeCode);

        if(b){
            // 1 查询购物车集合
            List<CartInfo> cartInfos = cartService.cartListByChecked(userId);
            // 2 生成订单对象/生成全局订单号(记录订单，对外交互)
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderStatus(OrderStatus.UNPAID.getComment());
            // atguigu+当前时间字符串+当前毫秒时间戳
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String format = sdf.format(new Date());
            String outTradeNo = "ATGUIGU"+format+System.currentTimeMillis();
            orderInfo.setOutTradeNo(outTradeNo);

            orderInfo.setUserId(userId);
            orderInfo.setTotalAmount(getTotlePrice(cartInfos));
            orderInfo.setOrderComment("硅谷快递，商品没有");
            // 过期时间，当前时间24小时候订单过期
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,1);
            Date time = c.getTime();
            orderInfo.setExpireTime(time);
            orderInfo.setDeliveryAddress(userAddress.getUserAddress());
            orderInfo.setCreateTime(new Date());
            orderInfo.setConsigneeTel(userAddress.getPhoneNum());
            orderInfo.setProcessStatus(OrderStatus.UNPAID.getComment());

            // 3 生成订单详情，将购物车对象封装到订单详情列表
            ArrayList<OrderDetail> orderDetails = new ArrayList<>();
            for (CartInfo cartInfo : cartInfos) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());

                // 验价
                boolean bool = skuService.checkPrice(cartInfo);
                if(!bool){
                    return "tradeFail";
                }

                // 验库存
                String url = "http://gware.gmall.com:9001/hasStock?skuId="+orderDetail.getSkuId()+"&num="+orderDetail.getSkuNum();
                String hasStock = HttpClientUtil.doGet(url);

                if(hasStock.equals("0")){
                    return "tradeFail";
                }

                orderDetails.add(orderDetail);
            }
            orderInfo.setOrderDetailList(orderDetails);

            // 4 保存订单
            OrderInfo info = orderService.saveOrder(orderInfo);

            // 5 删除购物车中已经提交转移到订单数据中的数据
            cartService.deleteCartInfos(cartInfos);

            return "redirect://payment.gmall.com:8087/index?orderId="+info.getId()+"&totalAmount"+info.getTotalAmount();//6 重定向到支付服务系统
        }else{

            return "tradeFail";
        }


    }

    /***
     * 结算功能(不发生数据的改变)
     * @param request
     * @param map
     * @return
     */
    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request, ModelMap map){
        String userId = request.getAttribute("userId").toString();
        String nickName = request.getAttribute("nickName").toString();

        // 取出购物车数据
        List<CartInfo> cartInfos = new ArrayList<>();
        cartInfos = cartService.cartListByChecked(userId);

        // 将购物车数据转化为结算数据(订单列表)
        ArrayList<OrderDetail> orderDetails = new ArrayList<>();
        for (CartInfo cartInfo : cartInfos) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());

            orderDetails.add(orderDetail);
        }


        // 取出用户收获地址列表
        List<UserAddress> userAddressList = userService.getUserAddressList(userId);
        map.put("orderDetailList",orderDetails);
        map.put("userAddressList",userAddressList);
        map.put("totalAmount",getTotlePrice(cartInfos));

        // 生成交易码,保存到redis一份儿
        String tradeCode = orderService.genTradeCode(userId);

        // 保存页面一份儿
        map.put("tradeCode",tradeCode);
        return "trade";
    }


    private BigDecimal getTotlePrice(List<CartInfo> cartInfos) {

        BigDecimal totalPrice = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfos) {
            if(cartInfo.getIsChecked().equals("1")){
                BigDecimal cartPrice = cartInfo.getCartPrice();
                totalPrice = totalPrice.add(cartPrice);
            }
        }
        return totalPrice;
    }
}
