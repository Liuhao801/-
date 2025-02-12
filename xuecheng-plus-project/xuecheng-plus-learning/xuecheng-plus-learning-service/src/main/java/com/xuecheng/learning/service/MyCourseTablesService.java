package com.xuecheng.learning.service;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;

public interface MyCourseTablesService {
    /**
     * 添加选课
     * @param userId 用户id
     * @param courseId 课程id
     * @return
     */
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    /**
     * 判断学习资格
     * @param userId 用户id
     * @param courseId 课程id
     * @return
     */
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId);

    /**
     * 保存选课信息到我的课程表
     * @param choosecourseId
     * @return
     */
    public Boolean saveChooseCourseStauts(Long choosecourseId);

    /**
     * 我的课程表
     * @param params
     * @return
     */
    public PageResult<XcCourseTables> mycourestabls(MyCourseTableParams params);
}
