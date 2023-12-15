package com.xuecheng.media;


import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;

/**
 * 测试MinIO
 */
public class MinioTest {
    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://43.137.8.13:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    //上传文件
    @Test
    public void upload() throws Exception {
        //根据扩展名取出mimeType
//        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
//        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
//        if(extensionMatch!=null){
//            mimeType = extensionMatch.getMimeType();
//        }

        UploadObjectArgs testbucket = UploadObjectArgs.builder()
                .bucket("xcbucket")
                .object("1.jpg")//添加子目录
                .filename("F:\\360MoveData\\Users\\HUAWEI\\Desktop\\新建文件夹\\1.jpg")
                .build();
        minioClient.uploadObject(testbucket);
        System.out.println("上传成功");

    }
    //删除文件
    @Test
    public void delete() throws Exception {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket("xcbucket").object("1.jpg").build();
        minioClient.removeObject(removeObjectArgs);
        System.out.println("删除成功");
    }

    //下载文件
    @Test
    public void get() throws Exception {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("xcbucket").object("1.jpg").build();
        FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
        FileOutputStream outputStream = new FileOutputStream(new File("F:\\360MoveData\\Users\\HUAWEI\\Desktop\\1.jpg"));
        IOUtils.copy(inputStream,outputStream);

        //校验文件的完整性对文件的内容进行md5
        FileInputStream fileInputStream1 = new FileInputStream(new File("F:\\360MoveData\\Users\\HUAWEI\\Desktop\\新建文件夹\\1.jpg"));
        String source_md5 = DigestUtils.md5Hex(fileInputStream1);
        FileInputStream fileInputStream = new FileInputStream(new File("F:\\360MoveData\\Users\\HUAWEI\\Desktop\\1.jpg"));
        String local_md5 = DigestUtils.md5Hex(fileInputStream);
        if(source_md5.equals(local_md5)){
            System.out.println("下载成功");
        }
    }


}

