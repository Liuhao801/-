package com.xuecheng.media;

import io.minio.Digest;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 大文件处理测试
 */
public class BigFileTest {

    @Test
    public void testChunk() throws IOException {
        //源文件
        File socrceFile=new File("E:\\java\\学成在线项目—资料\\ABC谋杀案.mkv");
        //分块文件路径
        String chunkPath="E:\\java\\学成在线项目—资料\\upload\\";
        //分块大小 5Mb
        long chunkSize=1024*1024*50;
        //分块数量
        long chunkNum= (long) Math.ceil(socrceFile.length()*1.0/chunkSize);

        //缓冲区
        byte[] bytes=new byte[1024];
        //使用RandomAccessFile读数据
        RandomAccessFile raf_r=new RandomAccessFile(socrceFile,"r");
        for (long i = 0; i < chunkNum; i++) {
            //创建分块文件
            File chunkFile=new File(chunkPath+i);
            //写入流
            RandomAccessFile raf_rw=new RandomAccessFile(chunkFile,"rw");
            int len=-1;
            while((len=raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,len);
                if(chunkFile.length()>=chunkSize){
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();
    }

    @Test
    public void testMerge() throws IOException {
        //源文件
        File socrceFile=new File("E:\\java\\学成在线项目—资料\\ABC谋杀案.mkv");
        //分块文件路径
        File chunkFolder=new File("E:\\java\\学成在线项目—资料\\upload\\");
        //合并后文件
        File mergeFile=new File("E:\\java\\学成在线项目—资料\\1.mkv");

        //取出分块文件
        File[] files = chunkFolder.listFiles();
        //转为list
        List<File> fileList = Arrays.asList(files);
        //排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });

        //写入流
        RandomAccessFile raf_rw=new RandomAccessFile(mergeFile,"rw");
        //缓存区
        byte[] bytes=new byte[1024];
        //遍历所有分块文件，向合并文件写
        for (File file : fileList) {
            //读入流
            RandomAccessFile raf_r=new RandomAccessFile(file,"r");
            int len=-1;
            while((len=raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,len);
            }
            raf_r.close();
        }
        raf_rw.close();

        //校验
        FileInputStream fileInputStream_merge=new FileInputStream(mergeFile);
        FileInputStream fileInputStream_source=new FileInputStream(socrceFile);
        String md5_merge = DigestUtils.md5Hex(fileInputStream_merge);
        String md5_source = DigestUtils.md5Hex(fileInputStream_source);
        if(md5_merge.equals(md5_source)){
            System.out.println("上传成功");
        }
    }
}
