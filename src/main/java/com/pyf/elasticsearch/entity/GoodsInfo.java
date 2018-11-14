package com.pyf.elasticsearch.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/8 14:24.
 */


@Document(indexName = "goodsinfo",type = "goods")
//indexName索引名称 可以理解为数据库名 必须为小写 不然会报org.elasticsearch.indices.InvalidIndexNameException异常
//type类型 可以理解为表名
@Data
public class GoodsInfo implements Serializable {
    @Id
    private String id;
    private String title;
    private String content;
    private int userId;
    private int weight;

}
