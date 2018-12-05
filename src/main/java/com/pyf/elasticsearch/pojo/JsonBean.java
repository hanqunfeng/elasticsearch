package com.pyf.elasticsearch.pojo;

import com.pyf.elasticsearch.entity.CnkiSpider;
import lombok.Data;

import java.util.List;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/30 17:52.
 */

@Data
public class JsonBean {

    private boolean result;
    private String error;
    private Long totalelements = 0l;
    private Integer totalpage = 0;
    private List<CnkiSpider> list;
    private SearchPojo searchPojo;

}
