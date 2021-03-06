package com.example.demo.service;

import com.example.demo.base.dao.OrderDao;
import com.example.demo.base.domain.MiaoshaOrder;
import com.example.demo.base.domain.MiaoshaUser;
import com.example.demo.base.domain.OrderInfo;
import com.example.demo.redis.OrderKey;
import com.example.demo.redis.RedisService;
import com.example.demo.base.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


@Service
public class OrderService {

    @Autowired
    OrderDao orderDao;

    @Autowired
    RedisService redisService;

    /**
     * 从缓存中获取秒杀订单
     * */
    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(long userId, long goodsId) {
        return redisService.get(OrderKey.getMiaoshaOrderByUidGid, "" + userId + "_" + goodsId, MiaoshaOrder.class);
    }

    /**
     * 获取订单信息
     * */
    public OrderInfo getOrderById(long orderId) {
        return orderDao.getOrderById(orderId);
    }


    /**
     * 创建订单
     * */
    @Transactional
    public OrderInfo createOrder(MiaoshaUser user, GoodsVo goods) {
        // 创建订单信息
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(goods.getId());
        orderInfo.setGoodsName(goods.getGoodsName());
        orderInfo.setGoodsPrice(goods.getMiaoshaPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(user.getId());
        orderDao.insert(orderInfo);

        // 创建秒杀订单信息
        MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
        miaoshaOrder.setGoodsId(goods.getId());
        miaoshaOrder.setOrderId(orderInfo.getId());
        miaoshaOrder.setUserId(user.getId());
        orderDao.insertMiaoshaOrder(miaoshaOrder);

        // Redis设置缓存
        redisService.set(OrderKey.getMiaoshaOrderByUidGid, "" + user.getId() + "_" + goods.getId(), miaoshaOrder);

        return orderInfo;
    }

    public void deleteOrders() {
        orderDao.deleteOrders();
        orderDao.deleteMiaoshaOrders();
    }

}
