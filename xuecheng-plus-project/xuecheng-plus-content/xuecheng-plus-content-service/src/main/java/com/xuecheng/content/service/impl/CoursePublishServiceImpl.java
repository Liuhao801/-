package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程发布相关接口
 */
@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;
    @Autowired
    private TeachplanService teachplanService;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    private CourseBaseMapper courseBaseMapper;

    /**
     * 获取课程预览信息
     * @param courseId 课程id
     * @return
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId){
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        //获取课程基本信息，营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);

        //获取课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setTeachplans(teachplanTree);

        return coursePreviewDto;
    }

    /**
     * @description 提交审核
     * @param courseId  课程id
     * @return
     */
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        //约束校验
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if(courseBaseInfo==null){
            XueChengPlusException.cast("找不到该课程");
        }
        //本机构只允许提交本机构的课程
        Long companyId1 = courseBaseInfo.getCompanyId();
        if(!companyId1.equals(companyId)){
            XueChengPlusException.cast("本机构只允许提交本机构的课程");
        }
        //状态为"已提交"的课程不允许再次提交
        String auditStatus = courseBaseInfo.getAuditStatus();
        if("202003".equals(auditStatus)){
            XueChengPlusException.cast("当前为等待审核状态,不允许再次提交");
        }
        //课程图片不能为空
        String pic = courseBaseInfo.getPic();
        if(StringUtils.isEmpty(pic)){
            XueChengPlusException.cast("课程图片不能为空");
        }
        //课程计划不能为空
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if(teachplanTree==null||teachplanTree.size()==0){
            XueChengPlusException.cast("课程计划不能为空");
        }


        //将课程基本信息，营销信息，计划信息，师资信息插入预发布表
        CoursePublishPre coursePublishPre=new CoursePublishPre();
        //课程基本信息，部分营销信息
        BeanUtils.copyProperties(courseBaseInfo,coursePublishPre);
        //机构Id
        coursePublishPre.setCompanyId(companyId);
        //课程计划信息JSON
        String teachplanTreeString  = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeString);
        //课程营销信息JSON
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String courseMarketString = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketString);
        //审核状态为"已提交"
        coursePublishPre.setStatus("202003");
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());

        CoursePublishPre coursePublishPre1 = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre1==null){
            //新增
            coursePublishPreMapper.insert(coursePublishPre);
        }else{
            //修改
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        //修改课程基本信息表审核状态为"已提交"
        courseBaseInfo.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBaseInfo);
    }
}
