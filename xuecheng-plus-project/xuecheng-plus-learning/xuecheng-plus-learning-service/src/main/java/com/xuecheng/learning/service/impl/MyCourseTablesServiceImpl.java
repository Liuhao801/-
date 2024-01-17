package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {
    @Autowired
    private XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    private XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    private ContentServiceClient contentServiceClient;

    /**
     * 添加选课
     * @param userId 用户id
     * @param courseId 课程id
     * @return
     */
    @Transactional
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        //课程收费标准
        String charge = coursepublish.getCharge();
        //选课记录
        XcChooseCourse chooseCourse = null;
        if("201000".equals(charge)){//课程免费
            //添加免费课程
            chooseCourse  = addFreeCoruse(userId, coursepublish);
            //添加到我的课程表
            XcCourseTables xcCourseTables = addCourseTabls(chooseCourse);
        }else{
            //添加收费课程
            chooseCourse  = addChargeCoruse(userId, coursepublish);
        }

        //获取学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);

        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse,xcChooseCourseDto);
        //设置学习资格
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());
        return xcChooseCourseDto;

    }

    /**
     * 添加免费课程,免费课程加入选课记录表、我的课程表
     * @param userId
     * @param coursepublish
     * @return
     */
    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish) {
        //查询选课记录表是否存在免费的且选课成功的订单
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001")//免费课程
                .eq(XcChooseCourse::getStatus, "701001");//选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size()>0) {
            return xcChooseCourses.get(0);
        }

        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));

        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if(insert<=0){
            XueChengPlusException.cast("添加免费课程到选课记录表失败");
        }
        return xcChooseCourse;
    }

    /**
     * 添加到我的课程表
     * @param xcChooseCourse
     * @return
     */
    @Transactional
    public XcCourseTables addCourseTabls(XcChooseCourse xcChooseCourse){
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)){
            XueChengPlusException.cast("选课未成功，无法添加到课程表");
        }
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(xcCourseTables!=null){
            //课程已存在
            return xcCourseTables;
        }
        //添加到我的课程表
        XcCourseTables xcCourseTablesNew = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse,xcCourseTablesNew);
        xcCourseTablesNew.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTablesNew.setCreateDate(LocalDateTime.now());
        xcCourseTablesNew.setUpdateDate(LocalDateTime.now());
        xcCourseTablesNew.setCourseType(xcChooseCourse.getOrderType());

        int insert = xcCourseTablesMapper.insert(xcCourseTablesNew);
        if(insert<=0){
            XueChengPlusException.cast("添加到我的课程表失败");
        }
        return xcCourseTablesNew;
    }

    /**
     * 根据课程和用户查询我的课程表中某一门课程
     * @param userId 用户Id
     * @param courseId 课程Id
     * @return
     */
    public XcCourseTables getXcCourseTables(String userId,Long courseId){
        XcCourseTables xcCourseTables =
                xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getUserId, userId)
                .eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;
    }

    /**
     * 添加收费课程
     * @param userId
     * @param coursepublish
     * @return
     */
    public XcChooseCourse addChargeCoruse(String userId,CoursePublish coursepublish){
        //如果存在待支付交易记录直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")//收费订单
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size()>0) {
            return xcChooseCourses.get(0);
        }

        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002");//待支付

        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));

        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if(insert<=0){
            XueChengPlusException.cast("添加收费课程到选课记录表失败");
        }
        return xcChooseCourse;
    }

    /**
     * 判断学习资格
     * @param userId 用户id
     * @param courseId 课程id
     * @return
     * [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     */
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId){
        //返回结果
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();

        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if(xcCourseTables==null){
            //{"code":"702002","desc":"没有选课或选课后没有支付"}
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }

        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if(!isExpires){
            //{"code":"702001","desc":"正常学习"}
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;
        }else{
            //{"code":"702003","desc":"已过期需要申请续期或重新支付"}
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }
    }

    /**
     * 保存选课信息到我的课程表
     * @param choosecourseId
     * @return
     */
    @Transactional
    public Boolean saveChooseCourseStauts(Long choosecourseId){
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(choosecourseId);
        if(xcChooseCourse==null){
            log.error("找不到选课信息,courseId:{}",choosecourseId);
            return false;
        }
        //选课状态
        String status = xcChooseCourse.getStatus();
        //只有"未支付"的课程需要添加到我的课程表
        if("701002".equals(status)){
            //修改选课表状态
            xcChooseCourse.setStatus("701001");//选课成功
            int i = xcChooseCourseMapper.updateById(xcChooseCourse);
            if(i<=0){
                log.error("修改选课表状态失败,courseId:{}",choosecourseId);
                XueChengPlusException.cast("修改选课表状态失败");
            }
            //将课程信息插入我的课程表
            XcCourseTables xcCourseTables = addCourseTabls(xcChooseCourse);
        }
        return true;
    }

    /**
     * 我的课程表
     * @param params
     * @return
     */
    public PageResult<XcCourseTables> mycourestabls(MyCourseTableParams params){
        //当前页码
        int pageNo = params.getPage();
        //当前页记录数
        int pageSize = params.getSize();

        //分页条件
        Page<XcCourseTables> page = new Page<>(pageNo, pageSize);
        //查询条件
        LambdaQueryWrapper<XcCourseTables> queryWrapper = new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getUserId, params.getUserId());

        //分页结果
        Page<XcCourseTables> result = xcCourseTablesMapper.selectPage(page, queryWrapper);
        //分页结果列表
        List<XcCourseTables> records = result.getRecords();
        //总记录数
        long total = result.getTotal();

        //封装返回结果
        PageResult pageResult = new PageResult(records, total, pageNo, pageSize);
        return pageResult;
    }
}
