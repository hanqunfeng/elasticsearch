package com.pyf.elasticsearch.service;

import com.pyf.elasticsearch.utils.ChineseToPinYinUtil;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.*;
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
    private ElasticsearchTemplate elasticsearchTemplate;

    public abstract T makePojoInfo(SearchHit searchHit);

    //中文、拼音混合搜索
    private QueryBuilder chineseAndPinYinSearch(String words){

        //使用dis_max直接取多个query中，分数最高的那一个query的分数即可
        DisMaxQueryBuilder disMaxQueryBuilder=QueryBuilders.disMaxQuery();

        /**
         * 纯中文搜索，不做拼音转换,采用edge_ngram分词(优先级最高)
         * 权重* 5
         */
        QueryBuilder normSearchBuilder=QueryBuilders.matchQuery("content.ngram",words).analyzer("ngramSearchAnalyzer").boost(5f);

        /**
         * 拼音简写搜索
         * 1、分析key，转换为简写  case:  南京东路==>njdl，南京dl==>njdl，njdl==>njdl
         * 2、搜索匹配，必须完整匹配简写词干
         * 3、如果有中文前缀，则排序优先
         * 权重*1
         */
        String firstChar = ChineseToPinYinUtil.ToFirstChar(words);
        TermQueryBuilder pingYinSampleQueryBuilder = QueryBuilders.termQuery("content.SPY", firstChar);

        /**
         * 拼音简写包含匹配，如 njdl可以查出 "城市公牛 南京东路店"，虽然非南京东路开头
         * 权重*0.8
         */
        QueryBuilder  pingYinSampleContainQueryBuilder=null;
        if(firstChar.length()>1){
            pingYinSampleContainQueryBuilder=QueryBuilders.wildcardQuery("content.SPY", "*"+firstChar+"*").boost(0.8f);
        }

        /**
         * 拼音全拼搜索
         * 1、分析key，获取拼音词干   case :  南京东路==>[nan,jing,dong,lu]，南京donglu==>[nan,jing,dong,lu]
         * 2、搜索查询，必须匹配所有拼音词，如南京东路，则nan,jing,dong,lu四个词干必须完全匹配
         * 3、如果有中文前缀，则排序优先
         * 权重*1
         */
        QueryBuilder pingYinFullQueryBuilder=null;
        if(words.length()>1){
            pingYinFullQueryBuilder=QueryBuilders.matchPhraseQuery("content.FPY", words).analyzer("pinyiFullSearchAnalyzer");
        }

        /**
         * 完整包含关键字查询(优先级最低，只有以上四种方式查询无结果时才考虑）
         * 权重*0.8
         */
        QueryBuilder containSearchBuilder=QueryBuilders.matchQuery("content", words).analyzer("ikSearchAnalyzer").minimumShouldMatch("100%");

        disMaxQueryBuilder
                .add(normSearchBuilder)
                .add(pingYinSampleQueryBuilder)
                .add(containSearchBuilder);

        //以下两个对性能有一定的影响，故作此判定，单个字符不执行此类搜索
        if(pingYinFullQueryBuilder!=null){
            disMaxQueryBuilder.add(pingYinFullQueryBuilder);
        }
        if(pingYinSampleContainQueryBuilder!=null){
            disMaxQueryBuilder.add(pingYinSampleContainQueryBuilder);
        }

        return disMaxQueryBuilder;
    }






    public Page<T> highLigthQuery(final String searchMessage, Pageable pageable, Class<T> tClass) {

        AnalyzeRequest analyzeRequest = new AnalyzeRequest()
                .text(searchMessage)
                .analyzer("ik_max_word");
                //.analyzer("ik_smart");
                //.analyzer("standard"); //这个是采用默认的分词器

        //获取分词效果
        List<AnalyzeResponse.AnalyzeToken> tokens = elasticsearchTemplate.getClient().admin().indices().analyze(analyzeRequest).actionGet().getTokens();
        String[] tokenTerms = new String[tokens.size()];
        for (AnalyzeResponse.AnalyzeToken token : tokens) {
            System.out.println(token.getTerm());
        }

        //QueryBuilder queryBuilder = QueryBuilders.queryStringQuery(searchMessage);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        for (AnalyzeResponse.AnalyzeToken token : tokens) {
            boolQueryBuilder.should(QueryBuilders.wildcardQuery("content","*"+token.getTerm()+"*")).should(QueryBuilders.wildcardQuery("title","*"+token.getTerm()+"*"));
            //boolQueryBuilder.should(QueryBuilders.matchQuery("content.SPY",token.getTerm())).should(QueryBuilders.matchQuery("title.SPY",token.getTerm()));
        }

        boolQueryBuilder.should(QueryBuilders.queryStringQuery(searchMessage));


        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                //.withQuery(QueryBuilders.matchQuery(field, searchMessage))
                .withQuery(boolQueryBuilder)
                .withHighlightFields(new HighlightBuilder.Field("*").requireFieldMatch(false).preTags("<span class='highlight'>").postTags("</span>"))
                .withPageable(pageable).withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        Page<T> pageS = elasticsearchTemplate.queryForPage(searchQuery, tClass);
        Page<T> page = elasticsearchTemplate.queryForPage(searchQuery, tClass, new SearchResultMapper() {

            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                ArrayList<T> list = new ArrayList<T>();
                SearchHits hits = response.getHits();
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
                            String searchMs = searchMessage;

                            String highLightMessage = value.fragments()[0].toString();
                            highLightMessage = highLightMessage.replaceAll("</span><span class='highlight'>","");
                            String setMethodName = parSetName(key);
                            if(!setMethodName.contains(".keyword")&&!setMethodName.contains(".FPY")&&!setMethodName.contains(".SPY")&&!setMethodName.contains(".ngram")) {
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
                    return new AggregatedPageImpl<T>((List<T>) list,pageable,pageS.getTotalElements());
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
