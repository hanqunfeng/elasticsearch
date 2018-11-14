package com.pyf.elasticsearch.controller;

import com.pyf.elasticsearch.entity.CnkiSpider;
import com.pyf.elasticsearch.service.CnkiSpiderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/9 15:09.
 */

@Controller
public class IndexController {
    @Autowired
    private CnkiSpiderService spiderService;

    @GetMapping("/")
    public String index(){
        return "elasticsearch/search";
    }

    @RequestMapping("/search")
    public String search(String searchContent, Model model,  @RequestParam(required = false,defaultValue = "0") Integer pageNumber,  @RequestParam(required = false,defaultValue = "5")Integer pageSize){
        if(searchContent!=null&&!"".equals(searchContent)) {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<CnkiSpider> pages = spiderService.highLigthQuery(searchContent, pageable,CnkiSpider.class);
            if(pages!=null&&pages.getSize()>0) {
                model.addAttribute("datas", pages.getContent());
                model.addAttribute("totalelements", pages.getTotalElements());
                model.addAttribute("totalpage", pages.getTotalPages());
            }
            model.addAttribute("pageNumber", pageNumber);
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("searchContent", searchContent);
        }
        return "elasticsearch/search";
    }

    @RequestMapping("/get/{id}")
    @ResponseBody
    public CnkiSpider getObject(@PathVariable(name = "id") String id){
        return spiderService.getObjectById(id);
    }
}
