package com.lanzou.video;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;

public class ProjectService {

    public static final String SUFFIX = "_x";

    /**
     * @param projectPath eg:E:\BaiduNetdiskDownload\剑指Java面试-Offer直通车
     * @return
     */
    public String cutProject(String projectPath) {
        DateTime startTime = DateTime.now();

        File projectDirFile = FileUtil.file(projectPath);
        if (!FileUtil.isDirectory(projectDirFile)) {
            throw new IllegalArgumentException("请输入正确的项目地址！");
        }
        String outProjectPath = projectPath + SUFFIX;
        if (FileUtil.exist(outProjectPath)) {
            FileUtil.mkdir(outProjectPath);
        }

        findFileAndCutFile(projectDirFile, outProjectPath);
        System.out.println();
        System.out.println("视频剪切完成，执行二次查找大文件处理");
        additionalCutFile(new File(outProjectPath));
        System.out.println();
        System.out.println("视频剪切完成，执行重命名操作");

        findFileAndRenameFile(new File(outProjectPath), "zip");

        long useTime = DateTime.now().between(startTime, DateUnit.SECOND);
        System.out.println("执行完毕，耗时" + useTime + "秒");

        return String.valueOf(useTime);

    }

    /**
     * 递归的方式，将目录下的视频进行处理
     * 小于100M的，复制到新目录，
     * 大于100M的，执行视频切割
     *
     * @param file        目录或文件的file
     * @param outPutPath  输出目录文件夹地址
     */
    private void findFileAndCutFile(File file, String outPutPath) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles.length > 0) {
                for (int i = 0; i < childFiles.length; i++) {
                    findFileAndCutFile(childFiles[i], outPutPath);
                }

            }
        } else {

            String fullPathAndName = file.getAbsolutePath();

            String parentPath = file.getParent();
            //新地址
            //String newPath = StrUtil.replace(parentPath, projectPath, projectPath + SUFFIX);
            String newPath = outPutPath;
            if (!FileUtil.exist(newPath)) {
                FileUtil.mkdir(newPath);
            }

            FFMpegVideoCutUtil ffMpegVideoCutUtil = FFMpegVideoCutUtil.getInstance();

            String newFileName = file.getName();
            String[] s = StrUtil.split(newFileName, ".");
            String lastSuffix = s[s.length - 1];
            if (ffMpegVideoCutUtil.isBigThan(file, FFMpegVideoCutUtil.SIZE_100_M)) {
                newFileName = newPath + "\\" + newFileName + "_x_{}." + lastSuffix;
                ffMpegVideoCutUtil.calculateAndCut(fullPathAndName, newFileName);
            } else {
                newFileName = newPath + "\\" + newFileName + "_c." + lastSuffix;
                FileUtil.copy(file, new File(newFileName), true);
            }
//            System.out.println(fullPathAndName);
        }

    }


    /**
     * 追加操作，有时候切割过的视频，实际大小还是大于100M，需要再次处理
     *
     * @param file
     */
    private void additionalCutFile(File file) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles.length > 0) {
                for (int i = 0; i < childFiles.length; i++) {
                    additionalCutFile(childFiles[i]);
                }

            }
        } else {

            FFMpegVideoCutUtil ffMpegVideoCutUtil = FFMpegVideoCutUtil.getInstance();

            if (ffMpegVideoCutUtil.isBigThan(file, FFMpegVideoCutUtil.SIZE_100_M)) {
                String fullPathAndName = file.getAbsolutePath();
                System.out.println("体积超过100M，执行二次处理：" + fullPathAndName);

                String parentPath = file.getParent();
                //新地址
//                String newPath = StrUtil.replace(parentPath, projectPath, projectPath + SUFFIX);
//                if (!FileUtil.exist(newPath)) {
//                    FileUtil.mkdir(newPath);
//                }

                String newFileName = file.getName();
                String[] s = StrUtil.split(newFileName, ".");
                String lastSuffix = s[s.length - 1];

                newFileName = parentPath + "\\" + newFileName + "_x_{}." + lastSuffix;
                ffMpegVideoCutUtil.calculateAndCut(fullPathAndName, newFileName);

                //删除原视频
                FileUtil.del(file);
            }
        }

    }

    /**
     * 重命名
     *
     * @param file
     * @param suffix 后缀 zip
     */
    private void findFileAndRenameFile(File file, String suffix) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles.length > 0) {
                for (int i = 0; i < childFiles.length; i++) {
                    findFileAndRenameFile(childFiles[i], suffix);
                }

            }
        } else {

            String fileName = file.getName();
            if (fileName.endsWith(suffix)) {
                return;
            }
            fileName = fileName + "." + suffix;
            FileUtil.rename(file, fileName, false, true);
        }

    }

    public static void main(String[] args) {
        ProjectService service = new ProjectService();
        service.cutProject("E:\\BaiduNetdiskDownload\\剑指Java面试-Offer直通车");
    }


}
