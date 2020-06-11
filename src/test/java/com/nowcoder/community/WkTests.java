package com.nowcoder.community;

import java.io.IOException;

public class WkTests {
    public static void main(String[] args) {
        String cmd = "F:/javaweb/wkhtmltopdf/bin/wkhtmltoimage --quality 75 https://www.baidu.com F:/javaweb/projectSpace/data/wk-images/3.png";
        try {
            //下面的代码执行本地命令
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok.");//先打印出OK，后出现的图片
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
