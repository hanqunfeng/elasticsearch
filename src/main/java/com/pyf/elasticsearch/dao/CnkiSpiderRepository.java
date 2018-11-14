package com.pyf.elasticsearch.dao;

import com.pyf.elasticsearch.entity.CnkiSpider;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/12 12:43.
 */

@Component
public interface CnkiSpiderRepository extends ElasticsearchRepository<CnkiSpider,String> {
}
