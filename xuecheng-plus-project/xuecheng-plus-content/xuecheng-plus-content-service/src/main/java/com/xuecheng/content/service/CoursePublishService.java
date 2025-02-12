package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;

/**
 * 课程发布相关接口
 */
public interface CoursePublishService {

    /**
     * 获取课程预览信息
     * @param courseId 课程id
     * @return
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * @description 提交审核
     * @param courseId  课程id
     * @return
     */
    public void commitAudit(Long companyId,Long courseId);

    /**
     * 课程发布接口
     * @param companyId 机构id
     * @param courseId 课程id
     */
    public void publish(Long companyId, Long courseId);

    /**
     * 课程静态化
     * @param courseId  课程id
     * @return File 静态化文件
     */
    public File generateCourseHtml(Long courseId);

    /**
     * 上传课程静态化页面
     * @param file  静态化文件
     * @return void
     */
    public void uploadCourseHtml(Long courseId,File file);

    /**
     * 根据Id查询课程详细信息
     * @param courseId 课程Id
     * @return
     */
    public CoursePublish getCoursePublish(Long courseId);

    /**
     * 查询缓存中的课程信息
     * @param courseId
     * @return
     */
    public CoursePublish getCoursePublishCache(Long courseId);

}
