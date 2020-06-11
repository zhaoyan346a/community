package com.nowcoder.community.controller;

import com.nowcoder.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {
    @Autowired
    private DataService dataService;

    //返回data页面
    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }

    //统计指定日期范围的UV
    @RequestMapping(path = "/data/uv", method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate, Model model) {
        //统计uv
        long uvResult = dataService.caculateUV(startDate, endDate);
        model.addAttribute("uvResult", uvResult);
        model.addAttribute("uvStartDate", startDate);
        model.addAttribute("uvEndDate", endDate);
        return "forward:/data";//转发到/data
    }

    //统计指定日期范围的DAU
    @RequestMapping(path = "/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
                         @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate, Model model) {
        //统计DAU
        long dauResult = dataService.caculateDAU(startDate, endDate);
        model.addAttribute("dauResult", dauResult);
        model.addAttribute("dauStartDate", startDate);
        model.addAttribute("dauEndDate", endDate);
        return "forward:/data";//转发到/data
    }
}
