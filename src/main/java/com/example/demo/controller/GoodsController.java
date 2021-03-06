package com.example.demo.controller;

import com.example.demo.base.domain.MiaoshaUser;
import com.example.demo.redis.GoodsKey;
import com.example.demo.redis.RedisService;
import com.example.demo.result.CodeMsg;
import com.example.demo.result.Result;
import com.example.demo.service.GoodsService;
import com.example.demo.service.MiaoshaUserService;
import com.example.demo.base.vo.GoodsDetailVo;
import com.example.demo.base.vo.GoodsVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @Description: 商品模块
 * --------------------------------------
 * @ClassName: GoodsController.java
 * @Date: 2021/3/20 22:59
 * @SoftWare: IntelliJ IDEA
 * --------------------------------------
 * @Author: lixj
 * @Contact: lixj_zj@163.com
 **/
@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;

    @Autowired
    ApplicationContext applicationContext;

    /**
     * QPS:1267 load:15 mysql
     * 5000 * 10
     * QPS:2884, load:5
     */
    /**
     * 跳转商品列表页
     */
    @RequestMapping(value = "/to_list", produces = "text/html")
    @ResponseBody
    public String list(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user) {
        model.addAttribute("user", user);
        // 取缓存
        String html = redisService.get(GoodsKey.getGoodsList, "", String.class);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }

        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList", goodsList);

        WebContext springWebContext = new WebContext(request, response,
                request.getServletContext(),
                request.getLocale(), model.asMap());
        // 手动渲染页面
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list", springWebContext);
        if (!StringUtils.isEmpty(html)) {
            // 设置页面缓存 此时Redis中key为： GoodsKey:gl
            redisService.set(GoodsKey.getGoodsList, "", html);
        }
        return html;
    }


    /**
     *
     */
    @RequestMapping(value = "/to_detail2/{goodsId}", produces = "text/html")
    @ResponseBody
    public String detail2(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user,
                          @PathVariable("goodsId") long goodsId) {
        model.addAttribute("user", user);

        // 取缓存
        String html = redisService.get(GoodsKey.getGoodsDetail, "" + goodsId, String.class);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }
        // 手动渲染
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods", goods);

        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        // 倒计时
        int remainSeconds = 0;
        // 秒杀状态
        int miaoshaStatus = 0;
        if (now < startAt) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int) ((startAt - now) / 1000);
        } else if (now > endAt) {//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        } else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("miaoshaStatus", miaoshaStatus);
        model.addAttribute("remainSeconds", remainSeconds);

        WebContext springWebContext = new WebContext(request, response,
                request.getServletContext(),
                request.getLocale(), model.asMap());
        // 手动渲染页面
        html = thymeleafViewResolver.getTemplateEngine().process("goods_detail", springWebContext);
        if (!StringUtils.isEmpty(html)) {
            // 设置页面缓存 此时Redis中key: GoodsKey:gd1 或者 GoodsKey:gd2
            redisService.set(GoodsKey.getGoodsDetail, "" + goodsId, html);
        }
        return html;
    }



    /**
     *
     * */
    @RequestMapping(value = "/detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> detail(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user,
                                        @PathVariable("goodsId") long goodsId) {
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        if (null == goods) {
            model.addAttribute("errmsg", "查询异常...");
            return Result.error(CodeMsg.SERVER_ERROR);
        }

        long now = System.currentTimeMillis();
        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();

        // 秒杀状态
        int miaoshaStatus = 0;
        // 倒计时
        int remainSeconds = 0;
        if (now < startAt) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int) ((startAt - now) / 1000);
        } else if (now > endAt) {//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        } else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }

        // 构建秒杀商品和用户的vo
        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(user);
        vo.setRemainSeconds(remainSeconds);
        vo.setMiaoshaStatus(miaoshaStatus);
        return Result.success(vo);
    }


}
