package com.lanzou.download;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import com.lanzou.common.RetryUtils;

/**
 * By LiHaoran
 * QQ 1079991001
 */
public class Demo {

    public static void main(String[] args) {

        LanZouParseService t = new LanZouParseService();

        //蓝奏云短链接
        String url = "https://lanzous.com/ibwtftg";

        //如果不需要密码这个passWord参数为空 如果需要密码,请在passWord参数里面写入密码即可!
        String passWord = "3j6w";//如果设置了,密码这里必填

        DateTime startTime = DateTime.now();
        //使用重试工具类
        String ss = RetryUtils.run(100,10,new RetryUtils.RetryTask<String>() {
            @Override
            public String execute() {
                String downloadUrl = t.parse(url);
                if (downloadUrl.endsWith("file/0")){
                    throw new RuntimeException("获取失败");
                }
                return downloadUrl;
            }
        });
        System.out.println("蓝奏云真实地址为:" + ss);

        long useTime = DateTime.now().between(startTime, DateUnit.MS);
        System.out.println("执行完毕，耗时" + useTime + "ms");


    }
}
