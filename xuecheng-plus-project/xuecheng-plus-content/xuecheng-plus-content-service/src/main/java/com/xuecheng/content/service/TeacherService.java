package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * 教师管理业务接口
 */
public interface TeacherService {
    /**
     * 查询教师信息
     * @param courseId 课程Id
     * @return
     */
    List<CourseTeacher> list(Long courseId);

    /**
     * 新增或修改教师信息
     * @param courseTeacher 教师信息
     * @return
     */
    CourseTeacher saveOrUpdateTeacher(CourseTeacher courseTeacher);

    /**
     * 删除教师
     * @param courseId 课程id
     * @param id 教师id，即course_teacher表的主键
     */
    void deleteTeacher(Long courseId, Long id);
}
