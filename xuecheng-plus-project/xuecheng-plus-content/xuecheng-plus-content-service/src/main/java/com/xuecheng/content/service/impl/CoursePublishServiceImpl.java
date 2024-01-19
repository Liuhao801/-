package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignClient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.jws.Oneway;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 课程发布相关接口
 */
@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;
    @Autowired
    private TeachplanService teachplanService;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CoursePublishMapper coursePublishMapper;
    @Autowired
    private MqMessageService mqMessageService;
    @Autowired
    private MediaServiceClient mediaServiceClient;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取课程预览信息
     * @param courseId 课程id
     * @return
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId){
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        //获取课程基本信息，营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);

        //获取课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setTeachplans(teachplanTree);

        return coursePreviewDto;
    }

    /**
     * @description 提交审核
     * @param courseId  课程id
     * @return
     */
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        //约束校验
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if(courseBaseInfo==null){
            XueChengPlusException.cast("找不到该课程");
        }
        //本机构只允许提交本机构的课程
        Long companyId1 = courseBaseInfo.getCompanyId();
        if(!companyId1.equals(companyId)){
            XueChengPlusException.cast("只允许提交本机构的课程");
        }
        //状态为"已提交"的课程不允许再次提交
        String auditStatus = courseBaseInfo.getAuditStatus();
        if("202003".equals(auditStatus)){
            XueChengPlusException.cast("当前为等待审核状态,不允许再次提交");
        }
        //课程图片不能为空
        String pic = courseBaseInfo.getPic();
        if(StringUtils.isEmpty(pic)){
            XueChengPlusException.cast("课程图片不能为空");
        }
        //课程计划不能为空
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if(teachplanTree==null||teachplanTree.size()==0){
            XueChengPlusException.cast("课程计划不能为空");
        }


        //将课程基本信息，营销信息，计划信息，师资信息插入预发布表
        CoursePublishPre coursePublishPre=new CoursePublishPre();
        //课程基本信息，部分营销信息
        BeanUtils.copyProperties(courseBaseInfo,coursePublishPre);
        //机构Id
        coursePublishPre.setCompanyId(companyId);
        //课程计划信息JSON
        String teachplanTreeString  = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeString);
        //课程营销信息JSON
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String courseMarketString = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketString);
        //审核状态为"已提交"
        coursePublishPre.setStatus("202003");
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());

        CoursePublishPre coursePublishPre1 = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre1==null){
            //新增
            coursePublishPreMapper.insert(coursePublishPre);
        }else{
            //修改
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        //修改课程基本信息表审核状态为"已提交"
        courseBaseInfo.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBaseInfo);
    }

    /**
     * 课程发布接口
     * @param companyId 机构id
     * @param courseId 课程id
     */
    @Transactional
    public void publish(Long companyId, Long courseId) {
        //约束校验
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre==null){
            XueChengPlusException.cast("找不到该课程，不能发布");
        }
        //本机构只允许提交本机构的课程
        Long companyId1 = coursePublishPre.getCompanyId();
        if(!companyId1.equals(companyId)){
            XueChengPlusException.cast("只允许发布本机构的课程");
        }
        //课程没有通过审核，不能发布
        String status = coursePublishPre.getStatus();
        if(!"202004".equals(status)){
            XueChengPlusException.cast("课程没有通过审核，不能发布");
        }
        //保存课程发布信息
        saveCoursePublish(courseId);

        //保存消息表
        saveCoursePublishMessage(courseId);

        //删除课程预发布表数据
        coursePublishPreMapper.deleteById(courseId);
    }

    /**
     * 保存课程发布信息
     * @param courseId 课程Id
     */
    private void saveCoursePublish(Long courseId){
        //查询课程预发布表信息
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre==null){
            XueChengPlusException.cast("找不到该课程，不能发布");
        }
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        //修改发布状态为"已发布"
        coursePublish.setStatus("203002");
        CoursePublish coursePublish1 = coursePublishMapper.selectById(courseId);
        if(coursePublish1==null){
            //新增
            coursePublishMapper.insert(coursePublish);
        }else{
            //修改
            coursePublishMapper.updateById(coursePublish);
        }

        //更新课程基本信息表发布状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);
    }

    /**
     * 保存消息表记录
     * @param courseId 课程Id
     */
    private void saveCoursePublishMessage(Long courseId){
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage==null){XueChengPlusException.cast(CommonError.UNKOWN_ERROR)
            ;
        }
    }

    /**
     * 课程静态化
     * @param courseId  课程id
     * @return File 静态化文件
     */
    public File generateCourseHtml(Long courseId){
        //静态化文件
        File htmlFile  = null;

        try {
            //配置freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());

            //加载模板
            //选指定模板路径,classpath下templates下
            //得到classpath路径
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //设置字符编码
            configuration.setDefaultEncoding("utf-8");

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化
            //参数1：模板，参数2：数据模型
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
//            System.out.println(content);
            //将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(content);
            //创建静态化文件
            htmlFile = File.createTempFile("course",".html");
            log.debug("课程静态化，生成静态文件:{}",htmlFile.getAbsolutePath());
            //输出流
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("课程静态化异常:{}",e.toString());
            XueChengPlusException.cast("课程静态化异常");
        }

        return htmlFile;
    }

    /**
     * 上传课程静态化页面
     * @param file  静态化文件
     * @return void
     */
    public void uploadCourseHtml(Long courseId,File file){
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course = mediaServiceClient.upload(multipartFile, "course/"+courseId+".html");
        if(course==null){
            XueChengPlusException.cast("上传静态文件异常");
        }
    }

    /**
     * 根据Id查询课程详细信息
     * @param courseId 课程Id
     * @return
     */
    public CoursePublish getCoursePublish(Long courseId){
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        return coursePublish ;
    }

    /**
     * 查询缓存中的课程信息
     * @param courseId
     * @return
     */
    //解决缓存穿透
//    public CoursePublish getCoursePublishCache(Long courseId){
//        //先从redis中查询数据
//        Object jsonObj = redisTemplate.opsForValue().get("course" + courseId);
//        if(jsonObj!=null){
//            //直接返回数据
//            String json = jsonObj.toString();
//            //防止缓存穿透,对不存在的数据设置为null
//            if(json.equals("null")){
//                return null;
//            }
//            CoursePublish coursePublish = JSON.parseObject(json, CoursePublish.class);
//            return coursePublish;
//        }else{
//            //若redis中没有数据，查询数据库
//            System.out.println("从数据库查询数据...");
//            CoursePublish coursePublish = getCoursePublish(courseId);
////            if(coursePublish!=null){
////                //再将结果存入redis
////                redisTemplate.opsForValue().set("course" + courseId,JSON.toJSONString(coursePublish));
////            }
//            redisTemplate.opsForValue().set("course" + courseId,JSON.toJSONString(coursePublish),30, TimeUnit.SECONDS);
//            return coursePublish;
//        }
//    }

    //利用同步锁解决缓存击穿
    public CoursePublish getCoursePublishCache(Long courseId){
        //先从redis中查询数据
        Object jsonObj = redisTemplate.opsForValue().get("course" + courseId);
        if(jsonObj!=null){
            //直接返回数据
            String json = jsonObj.toString();
            //防止缓存穿透,对不存在的数据设置为null
            if(json.equals("null")){
                return null;
            }
            CoursePublish coursePublish = JSON.parseObject(json, CoursePublish.class);
            return coursePublish;
        }else{
            //若redis中没有数据，查询数据库
            synchronized (this){
            //再查一次缓存
            jsonObj = redisTemplate.opsForValue().get("course" + courseId);
            if(jsonObj!=null) {
                //直接返回数据
                String json = jsonObj.toString();
                //防止缓存穿透,对不存在的数据设置为null
                if (json.equals("null")) {
                    return null;
                }
                CoursePublish coursePublish = JSON.parseObject(json, CoursePublish.class);
                return coursePublish;
            }
                System.out.println("从数据库查询数据...");
                CoursePublish coursePublish = getCoursePublish(courseId);
                redisTemplate.opsForValue().set("course" + courseId,JSON.toJSONString(coursePublish),30, TimeUnit.SECONDS);
                return coursePublish;
            }
        }
    }
}
