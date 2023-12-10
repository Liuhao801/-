package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    /**
     * 课程分类树形结构查询
     * @param id 根节点id
     * @return
     */
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //获取递归查询分类结果
        List<CourseCategoryTreeDto> courseCategoryTreeDtos=courseCategoryMapper.selectTreeNodes(id);

        //封装结果
        //将查询结果转为map,方便查找父节点
        Map<String, CourseCategoryTreeDto> map = courseCategoryTreeDtos.stream().collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));

        //依次遍历每个元素
        courseCategoryTreeDtos.stream().forEach(item->{
            //找到当前节点的父节点
            CourseCategoryTreeDto parent = map.get(item.getParentid());
            if(parent!=null){
                if(parent.getChildrenTreeNodes()==null){
                    parent.setChildrenTreeNodes(new ArrayList<>());
                }
                //下边开始往ChildrenTreeNodes属性中放子节点
                parent.getChildrenTreeNodes().add(item);
            }
        });
        //返回根节点的子节点列表
        return map.get(id).getChildrenTreeNodes();
    }
}
