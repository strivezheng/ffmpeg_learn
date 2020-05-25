# ffmpeg_learn

#### 介绍
java利用ffmpeg处理视频

利用ffmpeg对视频进行处理，（目的是为了将视频文件存到蓝奏云，蓝奏云只支持小于100m的zip等文件）  
- FFMpegVideoCutUtil: 实现了基本对视频的剪切功能，根据视频长度和大小，将切割后的视频保持在100M以内  
- ProjectService：综合了FFMpegVideoCutUtil功能，实现了完整的功能  
1. 扫描文件夹，将小于100m的视频复制到新的文件夹  
2. 扫描文件夹，将大于100m的视频剪切成小于100m的  
3. 第2步的操作，有可能最后一个视频还是大于100m，再追加一次剪切操作  
4. 对所有视频进行修改后缀操作，改为zip
之前
![Image text](https://gitee.com/sinstar_889/ffmpeg_learn/raw/master/src/main/resources/img/befor.png)
之后
![这里随便写文字](https://gitee.com/sinstar_889/ffmpeg_learn/raw/master/src/main/resources/img/after.png)

#####lanzou网盘直链解析
- 功能类：LanZouParseService
- 示例demo，使用了重试工具类，避免一次解析不成功