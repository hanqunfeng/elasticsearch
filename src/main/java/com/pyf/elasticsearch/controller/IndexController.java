package com.pyf.elasticsearch.controller;

import com.pyf.elasticsearch.entity.CnkiSpider;
import com.pyf.elasticsearch.pojo.SearchPojo;
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
    public String index( Model model){
        SearchPojo searchPojo = new SearchPojo();
        searchPojo.setTag("全部");
        model.addAttribute("searchPojo", searchPojo);
        model.addAttribute("nocontent", true);
        return "elasticsearch/search";
    }

    @RequestMapping("/search")
    public String search(String searchContent, Model model, @RequestParam(required = false,defaultValue = "0") Integer pageNumber, @RequestParam(required = false,defaultValue = "5")Integer pageSize, SearchPojo searchPojo){
        if(searchContent!=null&&!"".equals(searchContent)) {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<CnkiSpider> pages = spiderService.highLigthQuery(searchContent, pageable,CnkiSpider.class,searchPojo);
            if(pages!=null&&pages.getSize()>0) {
                model.addAttribute("datas", pages.getContent());
                model.addAttribute("totalelements", pages.getTotalElements());
                model.addAttribute("totalpage", pages.getTotalPages());
            }else{//如果搜索内容只有一个英文单词，则给出查询建议
                String[] suggests = spiderService.getSuggest(searchContent);
                //suggests = new String[]{"standard","222","333"};
                if(suggests!=null) {
                    model.addAttribute("suggests", suggests);
                }
            }
            model.addAttribute("pageNumber", pageNumber);
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("searchContent", searchContent);
            model.addAttribute("nocontent", false);
        }else {
            model.addAttribute("nocontent", true);
        }

        String[] tags = spiderService.getTags();
        model.addAttribute("tags",tags);
        model.addAttribute("searchPojo",searchPojo);

        return "elasticsearch/search";
    }

    @RequestMapping("/get/{id}")
    @ResponseBody
    public CnkiSpider getObject(@PathVariable(name = "id") String id) {
        CnkiSpider cnkiSpider = spiderService.getObjectById(id);
        return cnkiSpider;
    }

}
