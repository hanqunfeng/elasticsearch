package com.pyf.elasticsearch.restController;

import com.pyf.elasticsearch.entity.CnkiSpider;
import com.pyf.elasticsearch.pojo.JsonBean;
import com.pyf.elasticsearch.pojo.SearchPojo;
import com.pyf.elasticsearch.service.CnkiSpiderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/30 17:42.
 */

@RestController
//@CrossOrigin(origins = {"*"},allowedHeaders = {"*"},methods = {RequestMethod.GET,RequestMethod.POST},allowCredentials = "true",maxAge = 3600l)
@CrossOrigin
public class IndexRestController {
    @Autowired
    private CnkiSpiderService spiderService;

    @RequestMapping("/essearch")
    public JsonBean restsearch(SearchPojo searchPojo){
        JsonBean jsonBean = new JsonBean();
        jsonBean.setSearchPojo(searchPojo);
        String searchContent = searchPojo.getSearchContent();
        if(searchContent!=null&&!"".equals(searchContent)) {
            int pageNum = 0;
            if(searchPojo.getPageNumber() == 0){
                searchPojo.setPageNumber(1);
            }
            if(searchPojo.getPageNumber() >= 1){
                pageNum = searchPojo.getPageNumber()-1;
            }
            Pageable pageable = PageRequest.of(pageNum, searchPojo.getPageSize());
            Page<CnkiSpider> pages = null;
            if(searchPojo.isHighLight()) {
                pages = spiderService.highLigthQuery(searchContent, pageable, CnkiSpider.class, searchPojo);
            }else {
                pages = spiderService.query(searchContent, pageable, CnkiSpider.class, searchPojo);
            }

            jsonBean.setResult(true);
            if (pages != null && pages.getSize() > 0) {
                jsonBean.setList(pages.getContent());
                jsonBean.setTotalelements(pages.getTotalElements());
                jsonBean.setTotalpage(pages.getTotalPages());
            }
        }else{
            jsonBean.setError("查询内容不能为空");
        }


        return jsonBean;
    }
    @RequestMapping("/esget/{id}")
    public CnkiSpider getById(@PathVariable String id){
        return spiderService.getObjectById(id);
    }
}
