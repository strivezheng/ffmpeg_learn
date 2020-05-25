package com.lanzou.video;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFMpegVideoCutUtil {

    private String ffmpegEXE = "D:\\utils\\ffmpeg.exe";

    private static final FFMpegVideoCutUtil instance = new FFMpegVideoCutUtil();


    public static long SIZE_95_M = 95 * 1024 * 1024;
    /**
     *
     */
    public static long SIZE_100_M = 100 * 1024 * 1024;
    public static long SIZE_190_M = 190 * 1024 * 1024;

    public synchronized static FFMpegVideoCutUtil getInstance() {
        return instance;
    }


    private FFMpegVideoCutUtil() {
    }


    /**
     * 获取视频总时间 s
     *
     * @param videoPath 视频路径
     * @return
     */
    public int getVideoTime(String videoPath) {
        List<String> commands = new ArrayList<String>();
        commands.add(ffmpegEXE);
        commands.add("-i");
        commands.add(videoPath);
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(commands);
            final Process p = builder.start();

            //从输入流中读取视频信息
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            //从视频信息中解析时长
            String regexDuration = "Duration: (.*?), start: (.*?), bitrate: (\\d*) kb\\/s";
            Pattern pattern = Pattern.compile(regexDuration);
            Matcher m = pattern.matcher(sb.toString());
            if (m.find()) {
                int time = getTimelen(m.group(1));
                System.out.println(videoPath + ",视频时长：" + time + ", 开始时间：" + m.group(2) + ",比特率：" + m.group(3) + "kb/s");
                return time;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    //格式:"00:00:10.68"
    private static int getTimelen(String timelen) {
        int min = 0;
        String strs[] = timelen.split(":");
        if (strs[0].compareTo("0") > 0) {
            min += Integer.valueOf(strs[0]) * 60 * 60;//秒
        }
        if (strs[1].compareTo("0") > 0) {
            min += Integer.valueOf(strs[1]) * 60;
        }
        if (strs[2].compareTo("0") > 0) {
            min += Math.round(Float.valueOf(strs[2]));
        }
        return min;
    }

    /**
     * 获取文件大小
     *
     * @param path
     * @return
     */
    public long getFileSize(String path) {
        File file = FileUtil.file(path);
        if (FileUtil.exist(file)) {
            return FileUtil.size(file);
        } else {
            return 0;
        }
    }

    /**
     * 判断文件大小是否大于该值
     *
     * @param file
     * @param compareSize
     * @return
     */
    public boolean isBigThan(File file, long compareSize) {

        if (!FileUtil.exist(file)) {
            return false;
        }
        long size = FileUtil.size(file);
        if (size > compareSize) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * 计算要裁剪的位置 eg：一个708s的视频，返回结果是[0, 354, 708]
     *
     * @param path
     * @return
     */
    public LinkedList<Long> calculateTime(String path) {
        LinkedList<Long> timeList = new LinkedList();

        long videoTime = getVideoTime(path);
        long size = getFileSize(path);
        if (size == 0) {
            throw new RuntimeException("大小为0");
        }

        //片段
        int segments = 0;
        if (size <= SIZE_100_M) {
            segments = 1;
        } else {
            BigDecimal bigDecimal = new BigDecimal(SIZE_100_M);
            BigDecimal sizeB = new BigDecimal(size);
            // 155M/100M=1.5  (1+1=2)
            segments = sizeB.divide(bigDecimal, 2, BigDecimal.ROUND_HALF_UP).intValue() + 1;
        }
        if (segments <= 1) {
            return timeList;
        }
        BigDecimal videoTimeB = new BigDecimal(videoTime);


        long eachTime = (long) (videoTime / segments);
//        timeList.add(0L);
        long currentTime = 0L;
        for (int i = 0; i <= segments; i++) {
            currentTime = i * eachTime;
            timeList.add(currentTime);
        }

        return timeList;
    }


    /**
     * 视频裁剪
     *
     * @param videoInputPath
     * @param videoOutputPath
     * @throws Exception
     */
    private void videoCut(String videoInputPath, String videoOutputPath, String from, String duration) throws Exception {
//		ffmpeg -i input.mp4 -y output.avi
        //
        List<String> command = new ArrayList<>();
        command.add(ffmpegEXE);

        command.add("-i");
        command.add("\"" + videoInputPath + "\""); //加双引号，防止路径有空格
        command.add("-ss");
        command.add(from);
        command.add("-t");
        command.add(duration);
        command.add("-acodec");
        command.add("copy");
        command.add("-vcodec");
        command.add("copy");
        command.add("\"" + videoOutputPath + "\"");


        for (String c : command) {
            System.out.print(c + " ");
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();

        InputStream errorStream = process.getErrorStream();
        InputStreamReader inputStreamReader = new InputStreamReader(errorStream);
        BufferedReader br = new BufferedReader(inputStreamReader);

        String line = "";
        while ((line = br.readLine()) != null) {
        }

        if (br != null) {
            br.close();
        }
        if (inputStreamReader != null) {
            inputStreamReader.close();
        }
        if (errorStream != null) {
            errorStream.close();
        }

    }


    /**
     * @param fullVideoPath 原文件地址
     * @param outPathFormat 输出文件地址：带{}格式化符号的，_x_{}.mp4
     */
    public void calculateAndCut(String fullVideoPath, String outPathFormat) {
        try {
            //708s
            //ffmpeg.getVideoTime("E:\\video\\2-10 socket相关_慕课网.mp4");
            LinkedList<Long> timeList = calculateTime(fullVideoPath);
            System.out.println(timeList);

            //ffmpeg.exe -i 2-10socket相关_慕课网.mp4 -ss 0 -t 354 -acodec copy -vcodec copy t.mp4
            //ffmpeg.videoCut(videoPath,"E:\\video\\t.mp4","0","400");
            //ffmpeg.videoCut(videoPath,"E:\\video\\t2.mp4","400","200");
            //[0, 354, 708]
            String fileName = FileUtil.getName(fullVideoPath);
            //String outPath = "E:\\video\\" + fileName + "_x_{}.mp4";
            for (int i = 0; i < timeList.size() - 1; i++) {
                String from = "";
                if (i == 0) {
                    from = String.valueOf(timeList.get(i));
                } else {
                    //除了第一段，后面的部分开始时间前移1s
                    from = String.valueOf(timeList.get(i) - 1);
                }

                String move = "";
                if (i == i - 1) {
                    //最后一段视频，多预留一秒，防止超过视频长度，文件损坏
                    move = String.valueOf(timeList.get(i + 1) - timeList.get(i) - 1);
                } else {
                    move = String.valueOf(timeList.get(i + 1) - timeList.get(i));
                }

                String fullOutPath = StrUtil.format(outPathFormat, i);
                //存在则删除
                if (FileUtil.exist(fullOutPath)) {
                    FileUtil.del(fullOutPath);
                }
                System.out.println("调用ffmpeg：");
                //执行切割
                videoCut(fullVideoPath, fullOutPath, from, move);
                System.out.println();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
