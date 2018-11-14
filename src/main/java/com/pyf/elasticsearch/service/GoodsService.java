package com.pyf.elasticsearch.service;

import com.pyf.elasticsearch.entity.GoodsInfo;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/9 15:24.
 */

@Service
public class GoodsService {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    public GoodsInfo makeGoodsInfo(SearchHit searchHit){
        GoodsInfo poem = new GoodsInfo();

        poem.setId(searchHit.getId());
        poem.setTitle(String.valueOf(searchHit.getSourceAsMap().get("title")));
        poem.setContent(String.valueOf(searchHit.getSourceAsMap().get("content")));
        poem.setUserId((Integer)(searchHit.getSourceAsMap().get("userId")));
        poem.setWeight((Integer)(searchHit.getSourceAsMap().get("weight")));
        return poem;
    }

    public Page<GoodsInfo> highLigthQuery(String searchMessage, Pageable pageable) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                //.withQuery(QueryBuilders.matchQuery(field, searchMessage))
                .withQuery(QueryBuilders.queryStringQuery(searchMessage))
                .withHighlightFields(new HighlightBuilder.Field("*").requireFieldMatch(false).preTags("<span class='highlight'>").postTags("</span>"))
                .withPageable(pageable)
                .build();
        Page<GoodsInfo> pageS = elasticsearchTemplate.queryForPage(searchQuery, GoodsInfo.class);
        Page<GoodsInfo> page = elasticsearchTemplate.queryForPage(searchQuery, GoodsInfo.class, new SearchResultMapper() {

            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                ArrayList<GoodsInfo> poems = new ArrayList<GoodsInfo>();
                SearchHits hits = response.getHits();
                for (SearchHit searchHit : hits) {
                    if (hits.getHits().length <= 0) {
                        return null;
                    }
                    GoodsInfo poem = makeGoodsInfo(searchHit);
                    // 反射调用set方法将高亮内容设置进去
                    try {
                        Map<String, HighlightField> map = searchHit.getHighlightFields();
                        for (String key : map.keySet()) {
                            //map.keySet()返回的是所有key的值
                            HighlightField value = map.get(key);//得到每个key对应的value值

                            String highLightMessage = value.fragments()[0].toString();
                            highLightMessage = highLightMessage.replaceAll("</span><span class='highlight'>","");
                            String setMethodName = parSetName(key);
                            Class<? extends GoodsInfo> poemClazz = poem.getClass();
                            Method setMethod = poemClazz.getMethod(setMethodName, String.class);
                            setMethod.invoke(poem, highLightMessage);

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    poems.add(poem);
                }
                if (poems.size() > 0) {
                    return new AggregatedPageImpl<T>((List<T>) poems,pageable,pageS.getTotalElements());
                }
                return null;
            }
        });
        return page;
    }

    /**
     * 拼接在某属性的 set方法
     *
     * @param fieldName
     * @return String
     */
    private  String parSetName(String fieldName) {
        if (null == fieldName || "".equals(fieldName)) {
            return null;
        }
        int startIndex = 0;
        if (fieldName.charAt(0) == '_')
            startIndex = 1;
        return "set" + fieldName.substring(startIndex, startIndex + 1).toUpperCase()
                + fieldName.substring(startIndex + 1);
    }

}
