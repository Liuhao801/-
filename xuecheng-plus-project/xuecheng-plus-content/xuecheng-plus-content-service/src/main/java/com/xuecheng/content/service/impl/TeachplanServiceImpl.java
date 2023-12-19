package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.ApiImplicitParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;

    /**
     * 查询课程计划树型结构
     * @param courseId 课程Id
     * @return
     */
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    /**
     * 新增或修改课程计划
     * @param teachplanDto
     */
    @Transactional
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        //课程计划id
        Long id = teachplanDto.getId();
        if(id==null){
            //新增
            Teachplan teachplanNew = new Teachplan();
            BeanUtils.copyProperties(teachplanDto,teachplanNew);
            //设置排序号
            Long courseId = teachplanDto.getCourseId();
            Long parentid = teachplanDto.getParentid();
            int count = getTeachplanCount(courseId, parentid);
            teachplanNew.setOrderby(count+1);

            teachplanMapper.insert(teachplanNew);
        }else{
            //修改
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    /**
     * 获取最新的排序号
     * @param courseId 课程Id
     * @param parentid 父课程Id
     * @return 排序号
     */
    private int getTeachplanCount(Long courseId,Long parentid){
        //select count(1) from teachplan where courseId=#{courseId} and parentid=#{parentid}
        LambdaQueryWrapper<Teachplan> queryWrapper =new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId).eq(Teachplan::getParentid,parentid);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count;
    }

    /**
     * 删除课程计划
     * @param teachplanId 课程计划Id
     */
    @Transactional
    public void deleteTeachplan(Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("课程计划不存在");
        }

        //查询课程子计划
        Long courseId = teachplan.getCourseId();
        int count = getTeachplanCount(courseId, teachplanId);
        if(count>0){
            XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
        }

        //删除课程计划
        teachplanMapper.deleteById(teachplanId);
        //删除对应媒资关联信息
        LambdaQueryWrapper<TeachplanMedia> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId,teachplanId);
        teachplanMediaMapper.delete(queryWrapper);
    }

    /**
     * 移动课程计划
     * @param moveType 移动类型
     * @param teachplanId 移动的课程计划Id
     */
    public void moveTeachplan(String moveType, Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("课程计划不存在");
        }

        Long courseId = teachplan.getCourseId();
        Long parentid = teachplan.getParentid();
        Integer orderby = teachplan.getOrderby();

        int move=0;
        if("movedown".equals(moveType)){
            //下移
            move=1;
        }else if("moveup".equals(moveType)){
            //上移
            move=-1;
        }

        if(move!=0){
            LambdaQueryWrapper<Teachplan> queryWrapper=new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getCourseId,courseId).eq(Teachplan::getParentid,parentid).eq(Teachplan::getOrderby,orderby+move);
            Teachplan teachplanMove = teachplanMapper.selectOne(queryWrapper);
            if(teachplanMove!=null){
                //可移动
                teachplan.setOrderby(teachplanMove.getOrderby());
                teachplanMove.setOrderby(orderby);
                teachplanMapper.updateById(teachplan);
                teachplanMapper.updateById(teachplanMove);
            }
        }

    }

    /**
     * 教学计划绑定媒资
     * @param bindTeachplanMediaDto
     * @return
     */
    @Transactional
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto){
        //教学计划id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("教学计划不存在");
        }
        Integer grade = teachplan.getGrade();
        if(grade!=2){
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资文件");
        }
        //课程id
        Long courseId = teachplan.getCourseId();

        //先删除原来该教学计划绑定的媒资
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId,bindTeachplanMediaDto.getTeachplanId()));
        //再添加教学计划与媒资的绑定关系
        TeachplanMedia teachplanMedia=new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto,teachplanMedia);
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMediaMapper.insert(teachplanMedia);
    }
}
