# ffmpeg_learn
#### 项目背景
蓝奏网盘是一个免费不限速的网盘，但是上传文件有限制，只能传压缩包zip、exe、apk等文件，而且文件限制100M，一次偶然的机会我发现如果将一段视频文件的后缀改成.zip再上传到蓝奏网盘后，通过获取文件的直链，视频居然可以在线播放，那这样就意味着蓝奏网盘可以用来存放自己的视频了（或者作为视频床）。

比如：
> https://wwa.lanzous.com/i6zp4te

但是视频文件一般都很大，所以我这里通过ffmpeg，实现了对一个文件夹里的视频进行扫描然后切割处理。使其大小小于100M。再通过其他工具批量上传到网盘。


#### 介绍
java利用ffmpeg处理视频（主要是切割）

利用ffmpeg对视频进行处理，（目的是为了将视频文件存到蓝奏云，蓝奏云只支持小于100m的zip等文件）  
- FFMpegVideoCutUtil: 实现了基本对视频的剪切功能，根据视频长度和大小，将切割后的视频保持在100M以内  
- ProjectService：综合了FFMpegVideoCutUtil功能，实现了完整的功能  
1. 扫描文件夹，将小于100m的视频复制到新的文件夹  
2. 扫描文件夹，将大于100m的视频剪切成小于100m的  
3. 第2步的操作，有可能最后一个视频还是大于100m，再追加一次剪切操作  
4. 对所有视频进行修改后缀操作，改为zip

操作之前的原数据
![图片](https://gitee.com/sinstar_889/ffmpeg_learn/raw/master/src/main/resources/img/befor.png)
https://gitee.com/sinstar_889/ffmpeg_learn/raw/master/src/main/resources/img/befor.png


操作之后的数据
![图片](https://gitee.com/sinstar_889/ffmpeg_learn/raw/master/src/main/resources/img/after.png)
https://gitee.com/sinstar_889/ffmpeg_learn/raw/master/src/main/resources/img/after.png

#####lanzou网盘直链解析
- 功能类：LanZouParseService
- 示例demo，使用了重试工具类，避免一次解析不成功
