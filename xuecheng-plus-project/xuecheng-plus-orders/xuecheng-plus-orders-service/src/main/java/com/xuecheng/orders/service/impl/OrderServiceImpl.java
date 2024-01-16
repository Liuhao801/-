package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Correlation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单相关接口
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private XcOrdersMapper xcOrdersMapper;
    @Autowired
    private XcOrdersGoodsMapper xcOrdersGoodsMapper;
    @Autowired
    private XcPayRecordMapper xcPayRecordMapper;
    @Autowired
    private OrderServiceImpl currentProxy;
    @Autowired
    private MqMessageService mqMessageService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${pay.qrcodeurl}")
    String qrcodeurl;
    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    /**
     * 创建商品订单
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付交易记录(包括二维码)
     */
    @Transactional
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //添加商品订单
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);
        //添加支付交易记录
        XcPayRecord payRecord = createPayRecord(xcOrders);

        //生成二维码
        String qrCode = null;
        //订单号
        Long payNo = payRecord.getPayNo();
        try {
            //二维码跳转地址
            String url = String.format(qrcodeurl+"/orders/requestpay?payNo=%s", payNo);
            qrCode = new QRCodeUtil().createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);
        payRecordDto.setQrcode(qrCode);

        return payRecordDto;
    }

    /**
     * 添加商品订单
     * @param userId
     * @param addOrderDto
     * @return
     */
    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto){
        //幂等性判断，一条选课记录只能有一条订单信息
        XcOrders xcOrders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if(xcOrders!=null){
            return xcOrders;
        }
        //插入订单信息
        xcOrders=new XcOrders();
        //通过雪花算法生成唯一Id
        xcOrders.setId(IdWorkerUtils.getInstance().nextId());
        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001");//未支付
        xcOrders.setUserId(userId);
        xcOrders.setOrderType(addOrderDto.getOrderType());//购买课程
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(addOrderDto.getOutBusinessId());//选课记录Id
        int insert = xcOrdersMapper.insert(xcOrders);
        if(insert<=0){
            XueChengPlusException.cast("添加订单信息失败");
        }
        //插入订单明细信息
        //将订单明细封装为List
        Long ordersId = xcOrders.getId();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(addOrderDto.getOrderDetail(), XcOrdersGoods.class);
        //遍历List
        xcOrdersGoods.forEach(goods->{
            goods.setOrderId(ordersId);
            xcOrdersGoodsMapper.insert(goods);
        });
        return xcOrders;
    }

    /**
     * 根据业务id查询订单
     * @param businessId 选课表主键
     * @return
     */
    public XcOrders getOrderByBusinessId(String businessId) {
        XcOrders orders = xcOrdersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return orders;
    }

    /**
     * 添加支付交易记录
     * @param orders 订单信息
     * @return
     */
    @Transactional
    public XcPayRecord createPayRecord(XcOrders orders){
        if(orders==null){
            XueChengPlusException.cast("订单不存在");
        }
        if("600002".equals(orders.getStatus())){
            XueChengPlusException.cast("订单已支付");
        }
        XcPayRecord payRecord = new XcPayRecord();
        //生成支付交易流水号
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);
        payRecord.setOrderId(orders.getId());//商品订单号
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(orders.getUserId());
        int insert = xcPayRecordMapper.insert(payRecord);
        if(insert<=0){
            XueChengPlusException.cast("生成支付记录失败");
        }
        return payRecord;
    }

    /**
     * 查询支付交易记录
     * @param payNo  交易记录号
     * @return
     */
    public XcPayRecord getPayRecordByPayno(String payNo){
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付记录id
     * @return 支付记录信息
     */
    public PayRecordDto queryPayResult(String payNo){
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XueChengPlusException.cast("查询不到订单信息");
        }
        //支付状态
        String status = payRecord.getStatus();
        //如果支付成功直接返回
        if ("601002".equals(status)) {
            PayRecordDto payRecordDto = new PayRecordDto();
            BeanUtils.copyProperties(payRecord, payRecordDto);
            return payRecordDto;
        }
        //从支付宝查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        //保存支付结果
        currentProxy.saveAliPayStatus( payStatusDto);
        //重新查询支付记录
        payRecord = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        return payRecordDto;
    }

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo){
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                XueChengPlusException.cast("请求支付宝查询订单结果失败");
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            XueChengPlusException.cast("请求支付宝查询订单结果失败");
        }

        String resultJson = response.getBody();
        //转map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        //支付结果状态
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        //支付金额
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        //支付宝订单号
        String trade_no = (String) alipay_trade_query_response.get("trade_no");

        //封装查询结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);
        payStatusDto.setTotal_amount(total_amount);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        return payStatusDto;

    }

    /**
     * 保存支付宝支付结果
     * @param payStatusDto  支付结果信息
     * @return void
     */
    @Transactional
    public void saveAliPayStatus(PayStatusDto payStatusDto){
        //支付记录号
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if(payRecord==null){
            XueChengPlusException.cast("找不到支付信息");
        }
        //订单号
        Long orderId = payRecord.getOrderId();
        XcOrders xcOrders = xcOrdersMapper.selectById(orderId);
        if(xcOrders==null){
            XueChengPlusException.cast("找不到订单信息");
        }
        //支付状态
        String status = payRecord.getStatus();
        //如果支付成功直接返回
        if ("601002".equals(status)) {
            return;
        }

        //支付状态
        String trade_status = payStatusDto.getTrade_status();
        //如果支付成功
        if("TRADE_SUCCESS".equals(trade_status)){
            //更新支付记录表信息
            payRecord.setOutPayNo(payStatusDto.getTrade_no());//支付宝订单号
            payRecord.setOutPayChannel("Alipay");
            payRecord.setStatus("601002");//支付成功
            payRecord.setPaySuccessTime(LocalDateTime.now());
            int i = xcPayRecordMapper.updateById(payRecord);
            if(i<=0){
                XueChengPlusException.cast("保存支付记录信息失败");
            }
            //更新订单表信息
            xcOrders.setStatus("600002");//支付成功
            i = xcOrdersMapper.updateById(xcOrders);
            if(i<=0){
                XueChengPlusException.cast("保存订单信息失败");
            }

            //将购买成功的课程信息写入消息mq_message表
            //参数1：支付结果通知类型，2: 业务id，3:业务类型
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify", xcOrders.getOutBusinessId(), xcOrders.getOrderType(), null);
            //发送消息
            notifyPayResult(mqMessage);
        }
    }

    /**
     * 发送通知结果
     * @param message 消息体
     */
    public void notifyPayResult(MqMessage message){
        //将消息转JSON
        String jsonString = JSON.toJSONString(message);
        //生成持久化的消息
        Message messageObj = MessageBuilder.withBody(jsonString.getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
        //全局唯一的消息ID，需要封装到CorrelationData中
        CorrelationData correlationData=new CorrelationData(message.getId().toString());
        //添加callback
        correlationData.getFuture().addCallback(result->{
            if(result.isAck()){
                //ack，发送消息成功
                log.info("发送消息成功:{}",jsonString);
                //删除mq_message表中的消息
                mqMessageService.completed(message.getId());
            }else{
                //发送消息失败
                log.error("发送消息失败:{}",jsonString);
            }
        },ex->{
            //发送消息异常
            log.error("发送消息异常:{}",jsonString);
        });
        //发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT,"",messageObj,correlationData);
    }
}
