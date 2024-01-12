package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 课程预览，发布
 */
@Controller
public class CoursePublishController {
    @Autowired
    private CoursePublishService coursePublishService;

    @ApiOperation("课程预览")
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId){
        //获取模型数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);

        ModelAndView modelAndView = new ModelAndView();
        //模型数据
        modelAndView.addObject("model",coursePreviewInfo);
        //模板
        modelAndView.setViewName("course_template");
        return modelAndView;
    }

    @ApiOperation("提交审核")
    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId){
        Long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId,courseId);
    }

    @ApiOperation("课程发布")
    @ResponseBody
    @PostMapping ("/coursepublish/{courseId}")
    public void coursepublish(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.publish(companyId,courseId);
    }

    @ApiOperation("查询课程发布信息")
    @ResponseBody
    @GetMapping("/r/coursepublish/{courseId}")
    public CoursePublish getCoursepublish(@PathVariable("courseId") Long courseId) {
        CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
        return coursePublish;
    }

//    @GetMapping("/test")
//    public void generateCourseHtml() throws IOException, TemplateException {
//        //配置freemarker
//        Configuration configuration = new Configuration(Configuration.getVersion());
//
//        //加载模板
//        //选指定模板路径,classpath下templates下
//        //得到classpath路径
//        String classpath = this.getClass().getResource("/").getPath();
//        configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
//        //设置字符编码
//        configuration.setDefaultEncoding("utf-8");
//
//        //指定模板文件名称
//        Template template = configuration.getTemplate("course_template.ftl");
//
//        //准备数据
//        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(125L);
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("model", coursePreviewInfo);
//
//        //静态化
//        //参数1：模板，参数2：数据模型
//        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
//
//        //输入流
//        InputStream inputStream = IOUtils.toInputStream(content);
//        //创建静态化文件
//        File file = new File("E:\\java\\XueCheng\\upload\\125.html");
//        //输出流
//        FileOutputStream outputStream = new FileOutputStream(file);
//        IOUtils.copy(inputStream, outputStream);
//    }
}
