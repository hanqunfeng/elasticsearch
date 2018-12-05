package com.pyf.elasticsearch.pojo;

import lombok.Data;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/21 14:28.
 */

@Data
public class SearchPojo {

    private String searchContent;
    private Integer pageNumber = 0;
    private Integer pageSize = 20;
    private boolean pinyin;
    private String tag = "全部";
    private boolean mustall;
    private boolean isHighLight;
}
