package com.lanzou.download;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;


/**
 * lanzou云 下载地址解析解析
 */
public class LanZouParseService {

    private String sign;
    private String preUrl;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko";
    private static final String HOST = "https://www.lanzous.com";


    /**
     * 解析地址
     *
     * @param url
     * @param passWord
     * @return
     */
    public String parse(String url, String passWord) {
        //开始解析了
        String preUrl = parsePageInfo(url);
        String ajaxUrl = parseDownloadInfo(preUrl);
        String downloadUrl = parseDownloadUrl(ajaxUrl, passWord);
        return downloadUrl;
    }

    /**
     * 解析地址
     *
     * @param url
     * @return
     */
    public String parse(String url) {
        return parse(url, null);
    }


    /**
     * 获取页面的第一部分，文件的描述以及宣传信息（没有下载地址）等
     *
     * @param url eg：https://www.lanzous.com/i37xlyd
     * @return
     */
    private String parsePageInfo(String url) {
        String pageData = sendGet(url, USER_AGENT);
        Document document = Jsoup.parse(pageData);
        Elements elements = document.getElementsByAttributeValue("class", "ifr2");
        if (CollectionUtil.isNotEmpty(elements)) {//
            this.preUrl = HOST + elements.get(0).attr("src");
        } else {
            elements = document.getElementsByAttributeValue("class", "n_downlink");
            this.preUrl = HOST + elements.get(0).attr("src");
            if (CollectionUtil.isEmpty(elements)){
                throw new RuntimeException("未获取到fn");
            }
        }
        //this.preUrl = HOST + "/fn?" + getSubString(pageData, "src=\"/fn?", "\"");
        return preUrl;
    }

    /**
     * 获取页面的第二部分：文件的下载地址
     *
     * @param url eg: https://www.lanzous.com/fn?UzVSOF40AmdTNlQ0AWMFNABpVHdTeFFuUGYDMFE7U2QJOFQ7WzJVNQhqVzI_c
     * @return 用于ajax 请求的url
     */
    private String parseDownloadInfo(String url) {
        String pageData = sendGet(url, USER_AGENT);
        this.sign = getSubString(pageData, "'sign':'", "'");
        String returnUrl = HOST + getSubString(pageData, "url : '", "'");
        return returnUrl;
    }

    /**
     * @param url eg:
     * @return
     */
    private String parseDownloadUrl(String url) {
        return parseDownloadUrl(url, "");
    }

    /**
     * 根据前面获取的信息，提取下载地址
     *
     * @param url      eg: https://www.lanzous.com/ajaxm.php
     * @param passWord
     * @return
     */
    private String parseDownloadUrl(String url, String passWord) {
        String data;
        if ("".equals(passWord)) {
            data = "action=downprocess&sign=" + this.sign;
        } else {
            data = "action=downprocess&sign=" + this.sign + "&p=" + passWord;
        }
        String result = sendGet(url, USER_AGENT, data, this.preUrl);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        String dom = jsonObject.getStr("dom");
        String fileUrl = jsonObject.getStr("url");
        String returnUrl = dom + "/file/" + fileUrl;
        //returnUrl = getSubString(result, "\"dom\":\"", "\",") + "/file/" + getSubString(result, "\"url\":\"", "\",\"");
        return returnUrl;
    }


    /**
     * 截取数据
     *
     * @param text  原始数据
     * @param left
     * @param right
     * @return
     */
    private String getSubString(String text, String left, String right) {
        String result = "";
        int zLen;
        if (left == null || left.isEmpty()) {
            zLen = 0;
        } else {
            zLen = text.indexOf(left);
            if (zLen > -1) {
                zLen += left.length();
            } else {
                zLen = 0;
            }
        }
        int yLen = text.indexOf(right, zLen);
        if (yLen < 0 || right == null || right.isEmpty()) {
            yLen = text.length();
        }
        result = text.substring(zLen, yLen);
        return result;
    }


    /**
     * 发送http请求
     *
     * @param url
     * @param ua
     * @return
     */
    private String sendGet(String url, String ua) {
        return sendGet(url, ua, "", "");
    }

    /**
     * 发送http请求
     *
     * @param url
     * @param ua
     * @param param
     * @param Referer
     * @return
     */
    private String sendGet(String url, String ua, String param, String Referer) {
        String result = "";
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url);
            //打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            //设置通用的请求属性
            connection.setRequestProperty("accept", "image/gif, image/jpeg, image/pjpeg, application/x-ms-application, application/xaml+xml, application/x-ms-xbap, */*");
            connection.setRequestProperty("Accept-Language", "en-us");
            connection.setRequestProperty("user-agent", ua);
            connection.setRequestProperty("Host", HOST);
            connection.setRequestProperty("Connection", "Keep-Alive");
            if (!"".equals(Referer)) {
                connection.setRequestProperty("Referer", Referer);
            }
            if (!"".equals(param)) {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                out = new PrintWriter(connection.getOutputStream());
                out.print(param);
                out.flush();
            }
            //建立实际的连接
            connection.connect();
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            //发送异常
            return "发送失败,请检查URL地址是否正确";
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                //关闭异常
                System.out.println("关闭异常");
            }
        }
        return result;
    }

}
