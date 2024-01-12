package com.xuecheng.media;


import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                .bucket("mediafiles")
                .object("course/125.html")//添加子目录
                .filename("E:\\java\\XueCheng\\upload\\125.html")
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

    //上传分块文件
    @Test
    public void test_uploadChunk() throws Exception{
        //分块文件路径
        File chunkFolder=new File("E:\\java\\学成在线项目—资料\\upload\\");
        //取出分块文件
        File[] files = chunkFolder.listFiles();
        for (int i = 0; i < files.length; i++) {
            //上传文件
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("video")  //桶
                    .object("ABC/"+i) //桶中文件名
                    .filename(files[i].getAbsolutePath())  //源文件
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("上传分块成功"+i);
        }
    }

    //调用minio接口，合并分块文件
    @Test
    public void test_merge() throws Exception{
//        List<ComposeSource> sources=new ArrayList<>();
//        for (int i = 0; i < 25; i++) {
//            ComposeSource composeSource = ComposeSource.builder().bucket("video").object("ABC/" + i).build();
//            sources.add(composeSource);
//        }
        //需要合并的文件
        List<ComposeSource> sources = Stream.iterate(0, i -> i++).limit(25).map(i -> ComposeSource.builder().bucket("video").object("ABC/" + i).build()).collect(Collectors.toList());
        //合并参数
        ComposeObjectArgs composeObjectArgs= ComposeObjectArgs.builder()
                .bucket("video")
                .object("ABC谋杀案.mkv")
                .sources(sources)  //需要合并的文件
                .build();
        minioClient.composeObject(composeObjectArgs);
    }

    /**
     * 清楚分块文件
     */
    @Test
    public void clearChunkFiles() throws Exception{

        String chunkFileFolderPath="b\\4\\b4e1dd897c4726e94f3b6f76f1822513\\chunk\\";
        int chunkTotal=3;

        List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                .collect(Collectors.toList());

        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket("video")
                        .objects(deleteObjects)
                        .build()
        );
        for (Result<DeleteError> result : results) {
            DeleteError error = result.get();
        }
    }
}

