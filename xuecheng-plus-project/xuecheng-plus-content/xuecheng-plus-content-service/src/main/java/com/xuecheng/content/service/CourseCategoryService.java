package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

/**
 * 课程分类接口
 */
public interface CourseCategoryService {
    /**
     * 课程分类树形结构查询
     * @param id 根节点id
     * @return
     */
    List<CourseCategoryTreeDto> queryTreeNodes(String id);
}
