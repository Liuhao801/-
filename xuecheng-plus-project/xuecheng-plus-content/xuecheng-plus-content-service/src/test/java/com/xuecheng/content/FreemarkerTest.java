package com.xuecheng.content;

import com.xuecheng.content.feignClient.MediaServiceClient;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.jnlp.FileOpenService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class FreemarkerTest {
    @Autowired
    private CoursePublishService coursePublishService;

    /**
     * 测试页面静态化
     */
    @Test
    public void testGenerateHtmlByTemplate() throws IOException, TemplateException {
        //配置freemarker
        Configuration configuration=new Configuration(Configuration.getVersion());

        //加载模块
        //获得指定模板路径
        String classPath = this.getClass().getResource("/").getPath();
        configuration.setDirectoryForTemplateLoading(new File(classPath+"/templates/"));
        //设置字符编码
        configuration.setDefaultEncoding("utf-8");
        //指定模板名称
        Template template = configuration.getTemplate("course_template.ftl");

        //模型数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(124L);
        Map<String,Object>map=new HashMap<>();
        map.put("model",coursePreviewInfo);

        //静态化
        //模板+数据
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        //将内容输出到文件中
        InputStream inputStream=IOUtils.toInputStream(content);
        //输出流
        FileOutputStream fileOutputStream = new FileOutputStream("E:\\java\\XueCheng\\upload\\test.html");
        IOUtils.copy(inputStream,fileOutputStream);
    }

    @Autowired
    MediaServiceClient mediaServiceClient;

    //远程调用，上传文件
    @Test
    public void test() {

        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("E:\\java\\XueCheng\\upload\\test.html"));
        mediaServiceClient.upload(multipartFile,"course/test.html");
    }
}
