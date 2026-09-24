package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 模型输出反馈：一条对应「某一次模型调用」，用来给模型打分。
 *
 * - 挂 callId 而不是 (runId, model)：同一个模型在一轮里可以出现多次（对照不同参数），
 *   挂到具体那次调用上，两张卡才能分别评；
 * - model 冗余一份，只为按模型聚合评分，省得回头再联表；
 * - emotion 取 positive / negative，再次提交同一情绪表示取消（与对话反馈一致）；
 * - 模型评分 = 该模型下 positive 与 negative 的对比，在统计里折算成百分比；
 * - 删除为标记删除：deletedTime=0 表示未删除。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class CompareFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer callId; // 被评价的那次调用（CompareCall.id）
    @Column
    private Integer runId; // 所属运行记录，便于整批清理
    @Column
    private String model; // 被评价的模型（冗余，供按模型聚合）
    @Column
    private String emotion; // positive-赞，negative-踩
    @Column
    private String tag; // 反馈标签
    @Column
    private String content; // 反馈说明
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;
    @Column
    private Long deletedTime; // 删除时间，0 表示未删除（标记删除）
    @Column
    private Integer deletedUid;

}
