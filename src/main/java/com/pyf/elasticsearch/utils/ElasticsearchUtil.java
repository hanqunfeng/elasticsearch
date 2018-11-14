package com.pyf.elasticsearch.utils;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import java.util.HashMap;
import java.util.Map;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/8 17:08.
 */


public class ElasticsearchUtil {

    public static QueryBuilder matchByKind(String kind, String key, Object value) {
        QueryBuilder queryBuilder = null;
        switch (kind) {
            //全文匹配，即所有字段都进行模糊分词匹配
            case "single":
                queryBuilder = QueryBuilders.queryStringQuery((String)value);
                break;
            //指定字段模糊分词匹配，先对词条进行分词，比如『落日熔金』，那么过滤条件就是这四个字的组合，比如落日熔金，落日，落，日，熔金，熔，金
            case "match":
                queryBuilder = QueryBuilders.matchQuery(key, value);
                break;
            //指定字段短语匹配，和match查询类似，match_phrase查询首先解析查询字符串来产生一个词条列表。然后会搜索所有的词条，但只保留包含了所有搜索词条的文档，并且词条的位置要邻接。一个针对短语“中华共和国”的查询不会匹配“中华人民共和国”，因为没有含有邻接在一起的“中华”和“共和国”词条。
            //这种完全匹配比较严格，类似于数据库里的“%落日熔金%”这种，使用场景比较狭窄。如果我们希望能不那么严格，譬如搜索“中华共和国”，希望带“我爱中华人民共和国”的也能出来，就是分词后，中间能间隔几个位置的也能查出来，可以使用slop参数。
            case "phraseMatch":
                queryBuilder = QueryBuilders.matchPhraseQuery(key, value);
                break;
            //这个是最严格的匹配，属于低级查询，不进行分词的,==
            case "term":
                queryBuilder = QueryBuilders.termQuery(key, value);
                break;
            //这个是最严格的匹配，属于低级查询，不进行分词的,==
            case "wildcard":
                queryBuilder = QueryBuilders.wildcardQuery(key, "*"+(String) value+"*");
                break;
            default:
                queryBuilder = QueryBuilders.matchQuery(key, value);

        }

        return queryBuilder;
    }


    public static SearchQuery getSearchQueryBySingle(String searchCongent){
        Map<String,Object> map = new HashMap<>();
        map.put("key",searchCongent);
        return getSearchQuery(map,"single");
    }



    public static SearchQuery getSearchQuery(Map<String, Object> map, String kind) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (String key : map.keySet()) {
            //map.keySet()返回的是所有key的值
            Object value = map.get(key);//得到每个key对应的value值
            //多个条件之间是and关系
            boolQueryBuilder.must(matchByKind(kind, key, value));
        }
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder).build();
        return searchQuery;
    }

    public static SearchQuery getSearchQuery(Map<String, Object> map, PageRequest pageRequest, String kind) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (String key : map.keySet()) {
            //map.keySet()返回的是所有key的值
            Object value = map.get(key);//得到每个key多对用value的值
            boolQueryBuilder.must(matchByKind(kind, key, value));
        }

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder).withPageable(pageRequest).build();
        return searchQuery;
    }

    //多个字段对应一个查询条件，条件之间是或的关系
    public static SearchQuery getSearchQuery(PageRequest pageRequest,Object queryContent,String... filedNames){
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(QueryBuilders.multiMatchQuery(queryContent,filedNames)).withPageable(pageRequest).build();
        return searchQuery;
    }

    public static SearchQuery getSearchQuery(Object queryContent,String... filedNames){
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(QueryBuilders.multiMatchQuery(queryContent,filedNames)).build();
        return searchQuery;
    }


}
