package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 在线学习相关接口
 */
@Slf4j
@Service
public class LearningServiceImpl implements LearningService {

    @Autowired
    private MyCourseTablesService myCourseTablesService;
    @Autowired
    private ContentServiceClient contentServiceClient;
    @Autowired
    private MediaServiceClient mediaServiceClient;

    /**
     * 获取教学视频
     * @param courseId 课程id
     * @param teachplanId 课程计划id
     * @param mediaId 视频文件id
     * @return
     */
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId){
        //获取课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if(coursepublish==null){
            return RestResponse.validfail("课程不存在");
        }

        //判断用户是否登录
        if(StringUtils.isNotEmpty(userId)){
            //用户已登录，判断该用户是否有学习资格
            XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(userId, courseId);
            String learnStatus = learningStatus.getLearnStatus();
            //[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
            if("702003".equals(learnStatus)){
                return RestResponse.validfail("已过期需要申请续期或重新支付");
            }else if("702001".equals(learnStatus)){
                //具有学习资格，远程调用媒资服务获得媒资地址
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            }
        }
        //用户未登录或未选课，判断该课程是否收费
        String charge = coursepublish.getCharge();
        if("201000".equals(charge)){
            //免费课程
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }
        return RestResponse.validfail("请购买课程后再来学习");
    }
}
