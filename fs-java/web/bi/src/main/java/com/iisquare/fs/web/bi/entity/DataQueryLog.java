package com.iisquare.fs.web.bi.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 数据查询日志：即席查询（SQL查询）、数据集查询、数据主题查询统一落一条记录，
 * 包含查询时间、查询用户、来源IP、请求地址、耗时与结果状态等关键信息，便于审计与问题排查。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class DataQueryLog {

    public static final String TYPE_OLAP = "olap"; // 即席查询
    public static final String TYPE_DATASET = "dataset"; // 数据集查询
    public static final String TYPE_THEME = "theme"; // 数据主题查询

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String type; // 查询类型：olap-即席查询，dataset-数据集查询，theme-数据主题查询
    @Column
    private Integer targetId; // 查询对象标识：数据主题查询为主题主键，即席查询为0
    @Column
    private String targetName; // 查询对象名称：数据集名称或数据主题名称
    @Column
    private String sqlText; // 实际执行的查询语句（已替换内置变量），列名 sql_text：sql 为数据库保留字
    @Column
    private Integer maxRows; // 行数限制，0表示不限制
    @Column
    private Integer timeout; // 超时时间（秒）
    @Column
    private Integer status; // 1-成功，2-失败
    @Column
    private Integer resultCode; // 业务返回码
    @Column
    private String message; // 结果描述或异常信息
    @Column
    private String detail; // 详细内容：失败时的异常原因（含根因）、SQL解析或数据集校验的明细
    @Column
    private Long rowCount; // 返回或影响行数
    @Column
    private Integer columnCount; // 返回列数
    @Column
    private Long duration; // 耗时（毫秒）
    @Column
    private String requestIp; // 来源IP
    @Column
    private String userAgent; // 客户端标识
    @Column
    private String requestUrl; // 请求地址
    @Column
    private Integer createdUid; // 查询用户标识，用户名称在查询时按标识填充
    @Column
    private Long createdTime; // 查询时间

}
