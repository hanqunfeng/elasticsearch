package com.pyf.elasticsearch.service;

import com.pyf.elasticsearch.dao.CnkiSpiderRepository;
import com.pyf.elasticsearch.entity.CnkiSpider;
import com.pyf.elasticsearch.utils.ArrayUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        cnkiSpider.setTags(String.valueOf(searchHit.getSourceAsMap().get("tags")));


        return cnkiSpider;
    }

    public CnkiSpider getObjectById(String id){
        return cnkiSpiderRepository.findById(id).get();
    }

    public String[] getTags(){
        String[] tags = {"新闻","文献"};

        return tags;
    }

    public String[] getSuggest(String searchContent){
        String[] suggestsall = {};
        String[] suggests = null;

        SearchResponse response =  elasticsearchTemplate.getClient().prepareSearch("cnki-6")
                .setQuery(QueryBuilders.matchAllQuery())
                .suggest(
                        new SuggestBuilder()
                                .setGlobalText(searchContent)
                                .addSuggestion("my_suggestion",new TermSuggestionBuilder("content").suggestMode(TermSuggestionBuilder.SuggestMode.ALWAYS))
                                //.addSuggestion("my_suggestion2",new TermSuggestionBuilder("title").suggestMode(TermSuggestionBuilder.SuggestMode.ALWAYS))
                )
                .execute().actionGet();

        List<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> list = response.getSuggest().getSuggestion("my_suggestion").getEntries();

        for(Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option> entry:list){
            System.out.println(entry.getText()+".Options:");
            suggests = new String[entry.getOptions().size()];
            int i = 0;
            for(Suggest.Suggestion.Entry.Option option:entry.getOptions()){
                System.out.println(option.getText());
                suggests[i] = option.getText().string();
                i++;
            }

            suggestsall = ArrayUtils.mergeStringArrays(suggestsall,suggests);
        }


        return suggestsall;
    }
}
