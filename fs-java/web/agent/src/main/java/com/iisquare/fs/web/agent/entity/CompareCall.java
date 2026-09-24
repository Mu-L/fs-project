package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 模型调用明细：一次提交里「一个模型一次调用」落一行。
 *
 * 它同时是两件事的唯一来源：
 * - 输出：正文与思考过程直接存在这里，运行记录不再整体存一份大 JSON；
 * - 统计：模型、状态、耗时、首字耗时都是可索引的小字段，聚合查询才走得动。
 *
 * - runId 指向当次提交（CompareRun），无外键约束，随运行记录一起标记删除；
 * - callKey 是页面上的本地键：同一个模型在一轮里可以出现多次（对照不同参数），
 *   靠它把「页面上那张卡片」和「这一行」对上，反馈也按 callId 定位；
 * - payload 是实际下发的参数快照，回溯时以它为准，而不是去关联可变的配置。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class CompareCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer runId; // 所属运行记录
    @Column
    private String callKey; // 页面本地键，用于卡片与调用行对齐
    @Column
    private String model; // 模型名称
    @Column
    private String state; // 本轮结果：finish-完成，abort-中断，error-失败
    @Column
    private Long duration; // 本轮耗时（毫秒）
    @Column
    private Long firstToken; // 首字耗时（毫秒），0 表示没收到增量
    @Column
    private Integer contentLength; // 正文长度，供「输出太短」这类排查参考
    @Column
    private Integer reasoningLength; // 思考长度
    @Column
    private String finishReason; // 结束原因
    @Column
    private String error; // 失败原因
    @Column
    private String payload; // 实际下发的参数快照（JSON）
    @Column
    private String content; // 正文
    @Column
    private String reasoning; // 思考过程
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;

}
