package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    CartInfoMapper cartInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void addToCart(CartInfo cartInfo) {

        CartInfo cartExist = new CartInfo();
        cartExist.setUserId(cartInfo.getUserId());
        cartExist.setSkuId(cartInfo.getSkuId());
        cartExist = cartInfoMapper.selectOne(cartExist);

        if(cartExist==null){
            // 插入数据库
            cartInfoMapper.insertSelective(cartInfo);
        }else{
            // 更新数据库
            cartInfo.setSkuNum(cartInfo.getSkuNum()+cartExist.getSkuNum());
            cartInfo.setCartPrice(cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
            cartInfo.setId(cartExist.getId());
            cartInfoMapper.updateByPrimaryKey(cartInfo);
        }


        // 同步缓存
        Jedis jedis = redisUtil.getJedis();

        String mapKey = "cart:"+cartInfo.getUserId()+":info";
        String cartKey = cartInfo.getId();
        String cartJson = JSON.toJSONString(cartInfo);
        jedis.hset(mapKey,cartKey,cartJson);

        jedis.close();
    }

    @Override
    public List<CartInfo> cartList(String userId) {

        List<CartInfo> cartInfos = new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();
        String mapKey = "cart:"+userId+":info";

        List<String> hvals = jedis.hvals(mapKey);

        if(hvals!=null){
            for (String hval : hvals) {
                CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
                cartInfos.add(cartInfo);
            }
        }else{
            // 查询数据库
            CartInfo cartInfo = new CartInfo();
            cartInfo.setUserId(userId);
            List<CartInfo> select = cartInfoMapper.select(cartInfo);
            cartInfos = select;

        }

        return cartInfos;
    }

    @Override
    public List<CartInfo> cartListByChecked(String userId) {

        List<CartInfo> cartInfos = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();
        String mapKey = "cart:"+userId+":info";
        List<String> hvals = jedis.hvals(mapKey);
        if(hvals!=null){
            for (String hval : hvals) {
                CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
                cartInfos.add(cartInfo);
            }
        }else{
            // 查询数据库
            CartInfo cartInfo = new CartInfo();
            cartInfo.setUserId(userId);
            List<CartInfo> select = cartInfoMapper.select(cartInfo);
            cartInfos = select;
        }
        Iterator<CartInfo> iterator = cartInfos.iterator();
        while(iterator.hasNext()){
            CartInfo cartInfo = iterator.next();

            if(cartInfo.getIsChecked().equals("0")){
                iterator.remove();
            }
        }
        return cartInfos;
    }

    @Override
    public List<CartInfo> checkCart(CartInfo cartInfo) {

        // 按照指定的字段修改数据
        // 指定规则Example
        // 修改updateCartInfo
        Example e = new Example(CartInfo.class);// update table set ... where c1 = ? and c2 = ?
        e.createCriteria().andEqualTo("userId",cartInfo.getUserId()).andEqualTo("skuId",cartInfo.getSkuId());

        CartInfo updateCartInfo = new CartInfo();
        updateCartInfo.setIsChecked(cartInfo.getIsChecked());
        cartInfoMapper.updateByExampleSelective(updateCartInfo,e);

        // 将更新后的最新数据同步到缓存
        CartInfo select = new CartInfo();
        select.setUserId(cartInfo.getUserId());
        select.setSkuId(cartInfo.getSkuId());
        CartInfo updateRedisOne = cartInfoMapper.selectOne(select);

        // 同步redis
        Jedis jedis = redisUtil.getJedis();
        String mapKey = "cart:"+cartInfo.getUserId()+":info";
        jedis.hset(mapKey,updateRedisOne.getId(),JSON.toJSONString(updateRedisOne));

        jedis.close();
        return null;
    }

    @Override
    public void mergeCart(String cartListCookie, String userId) {
        // db中的购物车数据
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> cartListDb = cartInfoMapper.select(cartInfo);

        if(StringUtils.isNotBlank(cartListCookie)){
            List<CartInfo> cartListCookieInfos = JSON.parseArray(cartListCookie, CartInfo.class);

            if(cartListDb!=null){
                for (CartInfo cartListCookieInfo : cartListCookieInfos) {
                    // 合并更新
                    boolean b = if_new_cart(cartListDb, cartListCookieInfo);
                    if(b){
                        cartListCookieInfo.setUserId(userId);
                        cartInfoMapper.insert(cartListCookieInfo);
                    }else{
                        for (CartInfo info : cartListDb) {
                            if(info.getSkuId().equals(cartListCookieInfo.getSkuId())){
                                info.setIsChecked(cartListCookieInfo.getIsChecked());
                                info.setSkuNum(cartListCookieInfo.getSkuNum());
                                info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));
                                cartInfoMapper.updateByPrimaryKeySelective(info);
                            }
                        }
                    }
                }
            }else{
                // 插入db
                for (CartInfo cartListCookieInfo : cartListCookieInfos) {
                    cartInfoMapper.insert(cartListCookieInfo);
                }
            }
        }


        // 同步缓存
        cartListDb = cartInfoMapper.select(cartInfo);
        if(cartListDb!=null){
            Jedis jedis = redisUtil.getJedis();
            String cartKey = "cart:"+userId+":info";
            HashMap<String, String> stringStringHashMap = new HashMap<>();
            for (CartInfo info : cartListDb) {
                stringStringHashMap.put(info.getId(),JSON.toJSONString(info));
            }
            jedis.hmset(cartKey,stringStringHashMap);
            jedis.close();
        }


    }

    @Override
    public void deleteCartInfos(List<CartInfo> cartInfos) {
        for (CartInfo cartInfo : cartInfos) {
            cartInfoMapper.deleteByPrimaryKey(cartInfo);
        }

        // 同步redis
        CartInfo cartInfo = new CartInfo();
        String userId = cartInfos.get(0).getUserId();
        cartInfo.setUserId(userId);
        List<CartInfo> select = cartInfoMapper.select(cartInfo);
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        for (CartInfo info : select) {
            stringStringHashMap.put(info.getId(),JSON.toJSONString(info));
        }
        Jedis jedis = redisUtil.getJedis();
        jedis.del("cart:"+userId+":info");
        jedis.hmset("cart:"+userId+":info",stringStringHashMap);
        jedis.close();
    }


    private boolean if_new_cart(List<CartInfo> cartInfos, CartInfo cartInfo) {
        boolean b = true;
        for (CartInfo info : cartInfos) {
            if(info.getSkuId().equals(cartInfo.getSkuId())){
                b = false;
            }
        }
        return b;
    }
}
