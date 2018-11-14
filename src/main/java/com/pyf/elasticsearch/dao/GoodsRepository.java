package com.pyf.elasticsearch.dao;

import com.pyf.elasticsearch.entity.GoodsInfo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/8 14:26.
 */


@Component
public interface GoodsRepository extends ElasticsearchRepository<GoodsInfo,String> {
}
