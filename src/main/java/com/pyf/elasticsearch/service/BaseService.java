package com.pyf.elasticsearch.service;

import com.pyf.elasticsearch.pojo.SearchPojo;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/12 12:49.
 */


public abstract class BaseService<T> {

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    /**
     * 从查询结果中封装查询对象，子类需要实现
     * @param searchHit
     * @return
     */
    public abstract T makePojoInfo(SearchHit searchHit);



    /**
     * 基于搜索关键字构建查询对象QueryBuilder
     * @param searchMessage
     * @return
     */
    private BoolQueryBuilder makeBoolQueryBuilder(final String searchMessage,SearchPojo searchPojo){
        AnalyzeRequest analyzeRequest = new AnalyzeRequest()
                .text(searchMessage)
                .analyzer("ik_max_word");
        //.analyzer("ik_smart");
        //.analyzer("standard"); //这个是采用默认的分词器

        //获取分词效果
        List<AnalyzeResponse.AnalyzeToken> tokens = elasticsearchTemplate.getClient().admin().indices().analyze(analyzeRequest).actionGet().getTokens();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        String regex = "^[a-z0-9A-Z]+$";

        for (AnalyzeResponse.AnalyzeToken token : tokens) {
            System.out.println(token.getTerm());

            if(searchPojo != null) {
                if(!searchPojo.isMustall()) {
                    if (searchPojo.isPinyin()) {
                        boolQueryBuilder.should(QueryBuilders.matchQuery("content.SPY", token.getTerm())).should(QueryBuilders.matchQuery("title.SPY", token.getTerm()));
                    } else {
                        if (token.getTerm().matches(regex)) {
                            boolQueryBuilder.should(QueryBuilders.wildcardQuery("content", "*" + token.getTerm() + "*")).should(QueryBuilders.wildcardQuery("title", "*" + token.getTerm() + "*"));
                        } else {
                            boolQueryBuilder.should(QueryBuilders.matchQuery("content", token.getTerm())).should(QueryBuilders.matchQuery("title", token.getTerm()));
                        }
                    }
                }else{
                    BoolQueryBuilder boolQueryBuilder_title = QueryBuilders.boolQuery();
                    BoolQueryBuilder boolQueryBuilder_content = QueryBuilders.boolQuery();
                    BoolQueryBuilder boolQueryBuilder_all = QueryBuilders.boolQuery();
                    if (searchPojo.isPinyin()) {
                        boolQueryBuilder_content.must(QueryBuilders.matchQuery("content.SPY", token.getTerm()));
                        boolQueryBuilder_title.must(QueryBuilders.matchQuery("title.SPY", token.getTerm()));
                        boolQueryBuilder_all.should(boolQueryBuilder_content).should(boolQueryBuilder_title);
                        boolQueryBuilder.must(boolQueryBuilder_all);
                    } else {
                        if (token.getTerm().matches(regex)) {
                            boolQueryBuilder_content.must(QueryBuilders.wildcardQuery("content", "*" + token.getTerm() + "*"));
                            boolQueryBuilder_title.must(QueryBuilders.wildcardQuery("title", "*" + token.getTerm() + "*"));
                            boolQueryBuilder_all.should(boolQueryBuilder_content).should(boolQueryBuilder_title);
                            boolQueryBuilder.must(boolQueryBuilder_all);
                        } else {
                            boolQueryBuilder_content.must(QueryBuilders.matchQuery("content", token.getTerm()));
                            boolQueryBuilder_title.must(QueryBuilders.matchQuery("title", token.getTerm()));
                            boolQueryBuilder_all.should(boolQueryBuilder_content).should(boolQueryBuilder_title);
                            boolQueryBuilder.must(boolQueryBuilder_all);
                        }
                    }
                }
            }


        }

        return boolQueryBuilder;

    }

    /**
     * 构建查询过滤器
     * @param searchPojo
     * @return
     */
    private QueryBuilder makeFilterBuilter(SearchPojo searchPojo){
        QueryBuilder filterBuilder = null;
        if (searchPojo != null && StringUtils.hasText(searchPojo.getTag()) && !searchPojo.getTag().equals("全部")) {
            //boolQueryBuilder.must(QueryBuilders.termQuery("tags.keyword", tag));
            filterBuilder = QueryBuilders.termQuery("tags", searchPojo.getTag());
        } else {
            filterBuilder = QueryBuilders.termsQuery("tags", "新闻", "文献");
        }
        return filterBuilder;
    }

    public Page<T> highLigthQuery(final String searchMessage, Pageable pageable, Class<T> tClass) {
        return highLigthQuery(searchMessage, pageable, tClass, null);
    }


    public Page<T> query(final String searchMessage, Pageable pageable, Class<T> tClass, SearchPojo searchPojo){
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(makeBoolQueryBuilder(searchMessage, searchPojo)).withFilter(makeFilterBuilter(searchPojo))
                .withPageable(pageable).withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        Page<T> page = elasticsearchTemplate.queryForPage(searchQuery, tClass);

        return page;
    }

    /**
     * 高亮显示查询逻辑
     * @param searchMessage
     * @param pageable
     * @param tClass
     * @param searchPojo
     * @return
     */
    public Page<T> highLigthQuery(final String searchMessage, Pageable pageable, Class<T> tClass, SearchPojo searchPojo) {

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(makeBoolQueryBuilder(searchMessage, searchPojo)).withFilter(makeFilterBuilter(searchPojo))
                .withHighlightFields(new HighlightBuilder.Field("*").requireFieldMatch(false).preTags("<span class='highlight'>").postTags("</span>"))
                .withPageable(pageable).withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        Page<T> page = elasticsearchTemplate.queryForPage(searchQuery, tClass, new SearchResultMapper() {

            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                ArrayList<T> list = new ArrayList<T>();
                SearchHits hits = response.getHits();
                long total = hits.getTotalHits();//总记录数
                //遍历当前页的记录
                for (SearchHit searchHit : hits) {
                    if (hits.getHits().length <= 0) {
                        return null;
                    }
                    T pojo = (T) makePojoInfo(searchHit);
                    // 反射调用set方法将高亮内容设置进去
                    try {
                        Map<String, HighlightField> map = searchHit.getHighlightFields();
                        for (String key : map.keySet()) {
                            //map.keySet()返回的是所有key的值
                            HighlightField value = map.get(key);//得到每个key对应的value值

                            String highLightMessage = value.fragments()[0].toString();
                            highLightMessage = highLightMessage.replaceAll("</span><span class='highlight'>", "");
                            String setMethodName = parSetName(key);
                            if (!setMethodName.contains(".keyword") && !setMethodName.contains(".FPY") && !setMethodName.contains(".SPY") && !setMethodName.contains(".ngram")) {
                                Method setMethod = tClass.getMethod(setMethodName, String.class);
                                setMethod.invoke(pojo, highLightMessage);
                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    list.add(pojo);
                }
                if (list.size() > 0) {
                    return new AggregatedPageImpl<T>((List<T>) list, pageable, total);
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
    private String parSetName(String fieldName) {
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
