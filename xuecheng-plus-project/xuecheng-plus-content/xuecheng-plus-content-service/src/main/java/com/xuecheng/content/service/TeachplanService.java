package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * 课程基本信息管理业务接口
 */
public interface TeachplanService {

    /**
     * 查询课程计划树型结构
     * @param courseId 课程Id
     * @return
     */
    List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 新增或修改课程计划
     * @param teachplanDto
     */
    void saveTeachplan(SaveTeachplanDto teachplanDto);

    /**
     * 删除课程计划
     * @param teachplanId 课程计划Id
     */
    void deleteTeachplan(Long teachplanId);

    /**
     * 移动课程计划
     * @param moveType 移动类型
     * @param teachplanId 移动的课程计划Id
     */
    void moveTeachplan(String moveType, Long teachplanId);

    /**
     * 教学计划绑定媒资
     * @param bindTeachplanMediaDto
     * @return
     */
    void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}
