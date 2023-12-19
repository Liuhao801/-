package com.xuecheng.media;

import org.junit.jupiter.api.Test;

/**
 * 视频处理测试类
 */
public class VideoTaskTest {
    @Test
    public void testCPU(){
        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println(processors);
    }
}
