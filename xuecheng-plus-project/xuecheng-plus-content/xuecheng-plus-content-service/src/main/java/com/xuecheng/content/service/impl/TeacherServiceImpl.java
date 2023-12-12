package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {
    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    /**
     * 查询教师信息
     * @param courseId 课程Id
     * @return
     */
    public List<CourseTeacher> list(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        return courseTeacherMapper.selectList(queryWrapper);
    }

    /**
     * 新增或修改教师信息
     * @param courseTeacher 教师信息
     * @return
     */
    @Transactional
    public CourseTeacher saveOrUpdateTeacher(CourseTeacher courseTeacher) {
        Long id = courseTeacher.getId();
        if(id==null){
            //新增
            //创建时间
            courseTeacher.setCreateDate(LocalDateTime.now());
            courseTeacherMapper.insert(courseTeacher);
            id=courseTeacher.getId();
        }else{
            //修改
            courseTeacherMapper.updateById(courseTeacher);
        }
        return courseTeacherMapper.selectById(id);
    }

    /**
     * 删除教师
     * @param courseId 课程id
     * @param id 教师id，即course_teacher表的主键
     */
    @Transactional
    public void deleteTeacher(Long courseId, Long id) {
        courseTeacherMapper.deleteById(id);
    }
}
