package com.xuecheng.content.jobHandler;

import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    private CoursePublishService coursePublishService;

    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",30,60);
    }

    /**
     * 课程发布任务处理
     * @param mqMessage 执行任务内容
     * @return
     */
    public boolean execute(MqMessage mqMessage) {
        //获取课程Id
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());
        //课程静态化
        generateCourseHtml(mqMessage,courseId);
        //课程索引
        saveCourseIndex(mqMessage,courseId);
        //课程缓存
        saveCourseCache(mqMessage,courseId);
        return true;
    }

    //生成课程静态化页面并上传至文件系统
    private void generateCourseHtml(MqMessage mqMessage,Long courseId){
        //任务Id
        Long taskId = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();

        //幂等性处理
        int stageOne = mqMessageService.getStageOne(taskId);
        if(stageOne>0){
            log.info("课程静态化已处理");
            return;
        }

        //课程静态化...
        //生成静态化页面
        File file = coursePublishService.generateCourseHtml(courseId);
        //上传静态化页面
        if(file!=null){
            coursePublishService.uploadCourseHtml(courseId,file);
        }
        //保存第一阶段状态
        mqMessageService.completedStageOne(taskId);
    }

    //保存课程索引信息
    private void saveCourseIndex(MqMessage mqMessage,Long courseId){
        //任务Id
        Long taskId = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();

        //幂等性处理
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if(stageTwo>0){
            log.info("保存课程索引信息已处理");
            return;
        }

        //保存课程索引信息

        //保存第二阶段状态
        mqMessageService.completedStageTwo(taskId);
    }

    //将课程信息缓存至redis
    private void saveCourseCache(MqMessage mqMessage,Long courseId){
        //任务Id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //幂等性处理
        int stageThree = mqMessageService.getStageThree(taskId);
        if(stageThree>0){
            log.info("课程信息缓存至redis已处理");
            return;
        }

        //将课程信息缓存至redis

        //保存第三阶段状态
        mqMessageService.completedStageThree(taskId);
    }
}
