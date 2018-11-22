package com.pyf.elasticsearch.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

/**
 * ${DESCRIPTION}
 * Created by hanqf on 2018/11/12 12:38.
 */

@Document(indexName = "cnki-6",type = "spider")
@Data
public class CnkiSpider implements Serializable {

    @Id
    private String id;
    private String title;
    private String content;
    private String authoer;
    private String link;
    private String source;
    private String sp_source;

    private String date;
    private String sp_date;

    private String tags;
}
