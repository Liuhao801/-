package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReceivePayNotifyService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MqMessageService mqMessageService;
    @Autowired
    private MyCourseTablesService myCourseTablesService;

    /**
     * 监听消息队列接收支付结果通知
     * @param message
     */
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //解析消息
        MqMessage mqMessage = JSON.parseObject(message.getBody(), MqMessage.class);
        //消息类型
        String messageType = mqMessage.getMessageType();
        //只接受对应queue的消息
        if(PayNotifyConfig.MESSAGE_TYPE.equals(messageType)){
            String orderType = mqMessage.getBusinessKey2();
            //只接受"购买课程"类型的消息
            if("60201".equals(orderType)){
                //选课记录Id
                Long chooseCourseId = Long.valueOf(mqMessage.getBusinessKey1());
                //保存选课信息到我的课程表
                Boolean b = myCourseTablesService.saveChooseCourseStauts(chooseCourseId);
                if(!b){
                    //保存课程信息失败
                    XueChengPlusException.cast("保存选课信息到我的课程表失败");
                }
            }
        }
    }
}
