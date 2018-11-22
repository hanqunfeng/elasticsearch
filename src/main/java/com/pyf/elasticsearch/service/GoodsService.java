package com.pyf.elasticsearch.service;

import com.pyf.elasticsearch.dao.GoodsRepository;
import com.pyf.elasticsearch.entity.GoodsInfo;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/9 15:24.
 */

@Service
public class GoodsService  extends BaseService<GoodsInfo> {
    @Autowired
    private GoodsRepository goodsRepository;



    public GoodsInfo makePojoInfo(SearchHit searchHit){
        GoodsInfo poem = new GoodsInfo();

        poem.setId(searchHit.getId());
        poem.setTitle(String.valueOf(searchHit.getSourceAsMap().get("title")));
        poem.setContent(String.valueOf(searchHit.getSourceAsMap().get("content")));
        poem.setUserId((Integer)(searchHit.getSourceAsMap().get("userId")));
        poem.setWeight((Integer)(searchHit.getSourceAsMap().get("weight")));
        try {
            poem.setCreateDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(String.valueOf(searchHit.getSourceAsMap().get("createDate"))));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return poem;
    }

    public Iterable<GoodsInfo> getListByDate(String startDate,String endDate){
        if(startDate==null){
            startDate = "2018-11-15 12:50:00";
        }
        if(endDate==null){
            endDate = "2018-11-15 12:56:59";
        }
        Iterable<GoodsInfo> iterable = goodsRepository.search(QueryBuilders.rangeQuery("createDate").format("yyyy-MM-dd HH:mm:ss").gte(startDate).lte(endDate));
        return iterable;
    }



}
