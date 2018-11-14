package com.pyf.elasticsearch.service;

import com.pyf.elasticsearch.dao.CnkiSpiderRepository;
import com.pyf.elasticsearch.entity.CnkiSpider;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/12 12:44.
 */

@Service
public class CnkiSpiderService extends BaseService<CnkiSpider> {

    @Autowired
    CnkiSpiderRepository cnkiSpiderRepository;

    @Override
    public CnkiSpider makePojoInfo(SearchHit searchHit) {
        CnkiSpider cnkiSpider = new CnkiSpider();
        cnkiSpider.setId(searchHit.getId());
        cnkiSpider.setTitle(String.valueOf(searchHit.getSourceAsMap().get("title")));
        cnkiSpider.setContent(String.valueOf(searchHit.getSourceAsMap().get("content")));
        cnkiSpider.setAuthoer(String.valueOf(searchHit.getSourceAsMap().get("authoer")));
        cnkiSpider.setLink(String.valueOf(searchHit.getSourceAsMap().get("link")));
        cnkiSpider.setSource(String.valueOf(searchHit.getSourceAsMap().get("source")));
        cnkiSpider.setSp_source(String.valueOf(searchHit.getSourceAsMap().get("sp_source")));
        cnkiSpider.setSp_date(String.valueOf(searchHit.getSourceAsMap().get("sp_date")));
        cnkiSpider.setDate(String.valueOf(searchHit.getSourceAsMap().get("date")));


        return cnkiSpider;
    }

    public CnkiSpider getObjectById(String id){
        return cnkiSpiderRepository.findById(id).get();
    }
}
