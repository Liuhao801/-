package com.xuecheng.media.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 视频处理任务类
 */
@Component
@Slf4j
public class VideoTask {
    @Autowired
    private MediaFileProcessService mediaFileProcessService;
    @Autowired
    private MediaFileService mediaFileService;

    //ffmpeg地址
    //@Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath="D:/ffmpeg/bin/ffmpeg.exe";

    /**
     * 视频处理任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        //获取CPU核数
        int processors = Runtime.getRuntime().availableProcessors();
        //获取任务列表
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardTotal, shardIndex, processors);
        //任务数
        int size = mediaProcessList.size();
        if(size==0){
            log.info("待处理任务数:"+size);
            return;
        }

        //开启线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //将任务加入线程池
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(()->{
                try {
                    //任务Id
                    Long id = mediaProcess.getId();
                    //抢占任务
                    boolean b = mediaFileProcessService.startTask(id);
                    if(!b){
                        //抢占任务失败
                        return;
                    }

                    //桶
                    String bucket = mediaProcess.getBucket();
                    //objectName
                    String filePath = mediaProcess.getFilePath();
                    //原视频md5
                    String fileId = mediaProcess.getFileId();
                    //下载avi文件到本地
                    File file = mediaFileService.downloadFileFromMinIO(bucket, filePath);
                    if(file==null){
                        log.info("下载待转码文件失败,bucket:{},objectName:{}",bucket,filePath);
                        mediaFileProcessService.saveProcessFinishStatus(id,"3",fileId,null,"下载待转码文件失败");
                        return;
                    }
                    log.info("下载待转码文件成功");

                    //文件转码
                    //创建临时MP4文件
                    File mp4File=null;
                    try{
                        mp4File=File.createTempFile("mp4",".mp4");
                    }catch (IOException e){
                        log.info("创建临时MP4文件失败");
                        mediaFileProcessService.saveProcessFinishStatus(id,"3",fileId,null,"创建临时MP4文件失败");
                        return;
                    }
                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = fileId+".mp4";
                    //转换后mp4文件的路径
                    String mp4_path = mp4File.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath,video_path,mp4_name,mp4_path);
                    //开始视频转换，成功将返回success
                    String result = videoUtil.generateMp4();
                    if(!result.equals("success")){
                        log.info("处理视频失败,视频地址:{},错误信息:{}", bucket + filePath, result);
                        mediaFileProcessService.saveProcessFinishStatus(id,"3",fileId,null,result);
                        return;
                    }
                    log.info("文件转码成功");

                    //将MP4文件上传到minio
                    //minio中存储路径
                    String objectName = getFilePath(fileId, ".mp4");
                    //url
                    String url="/"+bucket+"/"+objectName;
                    boolean b1 = mediaFileService.addMediaFilesToMinIO(mp4_path, "video/mp4", bucket, objectName);
                    if(!b1){
                        log.info("上传视频到minio失败,bucket:{},objectName:{}", bucket,objectName);
                        mediaFileProcessService.saveProcessFinishStatus(id,"3",fileId,null,"上传视频到minio失败");
                        return;
                    }
                    log.info("上传文件到minio成功");
                    //保存处理结果
                    mediaFileProcessService.saveProcessFinishStatus(id,"2",fileId,url,null);

                }finally {
                    //计数器减一
                    countDownLatch.countDown();
                }
            });
        });
        //等待,给一个充裕的超时时间,防止无限等待，到达超时时间还没有处理完成则结束任务
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    private String getFilePath(String fileMd5,String fileExt){
        return  fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }

}
