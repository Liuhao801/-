package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.TeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(value = "教师编辑接口",tags = "教师编辑接口")
public class TeacherController {
    @Autowired
    private TeacherService teacherService;

    @ApiOperation("查询教师信息")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> list(@PathVariable Long courseId){
        return teacherService.list(courseId);
    }

    @ApiOperation("新增教师信息")
    @PostMapping("/courseTeacher")
    public CourseTeacher saveTeacher(@RequestBody CourseTeacher courseTeacher){
        return teacherService.saveOrUpdateTeacher(courseTeacher);
    }

    @ApiOperation("修改教师信息")
    @PutMapping("/courseTeacher")
    public CourseTeacher updateTeacher(@RequestBody CourseTeacher courseTeacher){
        return teacherService.saveOrUpdateTeacher(courseTeacher);
    }

    @ApiOperation("删除教师")
    @DeleteMapping("/courseTeacher/course/{courseId}/{id}")
    public void deleteTeacher(@PathVariable Long courseId,@PathVariable Long id){
        teacherService.deleteTeacher(courseId,id);
    }
}
