package com.duitang.milanserver.service;

import java.io.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.duitang.milanserver.model.Onebest;
import com.duitang.milanserver.model.Word;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.log4j.PropertyConfigurator;

import com.alibaba.fastjson.JSON;
import com.iflytek.msp.cpdb.lfasr.client.LfasrClientImp;
import com.iflytek.msp.cpdb.lfasr.exception.LfasrException;
import com.iflytek.msp.cpdb.lfasr.model.LfasrType;
import com.iflytek.msp.cpdb.lfasr.model.Message;
import com.iflytek.msp.cpdb.lfasr.model.ProgressStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ProcessLfasr
{

    // 原始音频存放地址
    private String local_file;
    /*
     * 转写类型选择：标准版和电话版分别为：
     * LfasrType.LFASR_STANDARD_RECORDED_AUDIO 和 LfasrType.LFASR_TELEPHONY_RECORDED_AUDIO
     * */
    private static final LfasrType type = LfasrType.LFASR_STANDARD_RECORDED_AUDIO;

    private static final ArrayList<String> STOPNOTE = new ArrayList<>(Arrays.asList("，","。","？","！"));
    // 等待时长（秒）
    private static int sleepSecond = 20;

    @Async
    public void process(String path) {
        // 加载配置文件
        PropertyConfigurator.configure("log4j.properties");

        local_file = path+".mp3";

        //mp4 convert to mp3
        try {
            FFmpeg ffmpeg = new FFmpeg("/usr/local/bin/ffmpeg");
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(path)
                    .addOutput(local_file)
                    .done();
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(builder).run();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // 初始化LFASR实例
        LfasrClientImp lc = null;
        try {
            lc = LfasrClientImp.initLfasrClient();
        } catch (LfasrException e) {
            // 初始化异常，解析异常描述信息
            Message initMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + initMsg.getErr_no());
            System.out.println("failed=" + initMsg.getFailed());
        }

        // 获取上传任务ID
        String task_id = "";
        HashMap<String, String> params = new HashMap<>();
        params.put("has_participle", "true");
        try {
            // 上传音频文件
            Message uploadMsg = lc.lfasrUpload(local_file, type, params);

            // 判断返回值
            int ok = uploadMsg.getOk();
            if (ok == 0) {
                // 创建任务成功
                task_id = uploadMsg.getData();
                System.out.println("task_id=" + task_id);
            } else {
                // 创建任务失败-服务端异常
                System.out.println("ecode=" + uploadMsg.getErr_no());
                System.out.println("failed=" + uploadMsg.getFailed());
            }
        } catch (LfasrException e) {
            // 上传异常，解析异常描述信息
            Message uploadMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + uploadMsg.getErr_no());
            System.out.println("failed=" + uploadMsg.getFailed());
        }

        // 循环等待音频处理结果
        while (true) {
            try {
                // 睡眠1min。另外一个方案是让用户尝试多次获取，第一次假设等1分钟，获取成功后break；失败的话增加到2分钟再获取，获取成功后break；再失败的话加到4分钟；8分钟；⋯⋯
                Thread.sleep(sleepSecond * 1000);
                System.out.println("waiting ...");
            } catch (InterruptedException e) {
            }
            try {
                // 获取处理进度
                Message progressMsg = lc.lfasrGetProgress(task_id);

                // 如果返回状态不等于0，则任务失败
                if (progressMsg.getOk() != 0) {
                    System.out.println("task was fail. task_id:" + task_id);
                    System.out.println("ecode=" + progressMsg.getErr_no());
                    System.out.println("failed=" + progressMsg.getFailed());

                    // 服务端处理异常-服务端内部有重试机制（不排查极端无法恢复的任务）
                    // 客户端可根据实际情况选择：
                    // 1. 客户端循环重试获取进度
                    // 2. 退出程序，反馈问题
                    continue;
                } else {
                    ProgressStatus progressStatus = JSON.parseObject(progressMsg.getData(), ProgressStatus.class);
                    if (progressStatus.getStatus() == 9) {
                        // 处理完成
                        System.out.println("task was completed. task_id:" + task_id);
                        break;
                    } else {
                        // 未处理完成
                        System.out.println("task was incomplete. task_id:" + task_id + ", status:" + progressStatus.getDesc());
                        continue;
                    }
                }
            } catch (LfasrException e) {
                // 获取进度异常处理，根据返回信息排查问题后，再次进行获取
                Message progressMsg = JSON.parseObject(e.getMessage(), Message.class);
                System.out.println("ecode=" + progressMsg.getErr_no());
                System.out.println("failed=" + progressMsg.getFailed());
            }
        }

        // 获取任务结果
        try {
            Message resultMsg = lc.lfasrGetResult(task_id);


            // 如果返回状态等于0，则任务处理成功
            if (resultMsg.getOk() == 0) {
                // 打印转写结果
                System.out.println(resultMsg.getData());
                File file =new File(path+".srt");
                if(!file.exists()){
                    file.createNewFile();
                }
                //true = append file
                try {
                    OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
                    BufferedWriter bufferWritter = new BufferedWriter(write);
                    List<Onebest> onebests = JSON.parseArray(resultMsg.getData(),Onebest.class);
//                    String data = "";
//                    List<Onebest> onebests = JSON.parseArray(data,Onebest.class);
                    int i = 0;
                    String tempWords = "";
                    long begin = 0;
                    long now = 0;
                    if (onebests.size()>0){
                        for (Onebest onebest:onebests){
                            if(onebest.getOnebest().length()>20){//单句大于20字才会使用分词
                                List<Word> words = onebest.getWordsResultList();
                                begin = onebest.getBg();
                                now = begin;
                                for(Word word:words){
                                    tempWords = tempWords + word.getWordsName();
                                    if(STOPNOTE.contains(word.getWordsName())){//遇到标点符号直接断句
                                        i++;
                                        bufferWritter.write(String.valueOf(i));
                                        bufferWritter.newLine();
                                        bufferWritter.write(parse(now)+" --> "+parse(begin + word.getWordEd()*10));
                                        bufferWritter.newLine();
                                        bufferWritter.write(tempWords);
                                        bufferWritter.newLine();
                                        bufferWritter.newLine();
                                        tempWords = "";
                                        now = begin + word.getWordEd()*10;
                                    }else if(tempWords.length()>20){//20字以上断句
                                        i++;
                                        bufferWritter.write(String.valueOf(i));
                                        bufferWritter.newLine();
                                        bufferWritter.write(parse(now)+" --> "+parse(begin+word.getWordEd()*10));
                                        bufferWritter.newLine();
                                        bufferWritter.write(tempWords);
                                        bufferWritter.newLine();
                                        bufferWritter.newLine();
                                        tempWords = "";
                                        now = begin + word.getWordEd()*10;
                                    }
                                }
                                if (tempWords.length()>0){//剩余的写文件
                                    i++;
                                    bufferWritter.write(String.valueOf(i));
                                    bufferWritter.newLine();

                                    bufferWritter.write(parse(now)+" --> "+parse(onebest.getEd()));
                                    bufferWritter.newLine();
                                    bufferWritter.write(tempWords);
                                    bufferWritter.newLine();
                                    bufferWritter.newLine();
                                    tempWords = "";
                                }
                            }else{//直接通过句子写文件
                                i++;
                                bufferWritter.write(String.valueOf(i));
                                bufferWritter.newLine();
                                bufferWritter.write(parse(onebest.getBg())+" --> "+parse(onebest.getEd()));
                                bufferWritter.newLine();
                                bufferWritter.write(onebest.getOnebest());
                                bufferWritter.newLine();
                                bufferWritter.newLine();
                            }
                        }
                    }
                    bufferWritter.flush();
                    bufferWritter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // 转写失败，根据失败信息进行处理
                System.out.println("ecode=" + resultMsg.getErr_no());
                System.out.println("failed=" + resultMsg.getFailed());
            }
        } catch (LfasrException e) {
            // 获取结果异常处理，解析异常描述信息
            Message resultMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + resultMsg.getErr_no());
            System.out.println("failed=" + resultMsg.getFailed());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String parse(long time) {
        Duration d = Duration.of(time, ChronoUnit.MILLIS);
        long h = d.toHours(); // 得到小时数
        String sh = h<10 ? "0" + h : "" + h; // 假设仅仅有一位数。加上个0

        // 为了得到后面的分，秒，毫秒，我们要将小时减掉，否则取分钟的时候会连小时算进去
        d = d.minusHours(h);

        long min = d.toMinutes(); // 得到分钟
        String smin = min<10 ? "0" + min : "" + min;
        d = d.minusMinutes(min); // 减掉分钟
        long s = d.getSeconds(); // 得到秒，注意这里是getSeconds，没有toSeconds方法
        String ss = s<10 ? "0" + s : "" + s;
        d = d.minusSeconds(s); // 减掉秒
        long m = d.toMillis(); // 得到毫秒
        String sm = m<10 ? "00" + m : (m<100 ? "0" + m : "" + m);
        return sh + ":" + smin + ":" + ss + "," + sm;
    }


}

