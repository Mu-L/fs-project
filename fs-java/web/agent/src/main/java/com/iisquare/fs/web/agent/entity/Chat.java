package com.iisquare.fs.web.agent.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 对话会话：多轮对话的容器，一次会话一条记录。
 *
 * - type 区分用途：agentic-发布应用（用户对话页），agentic_draft-调试运行（设计器调试抽屉），
 *   两类会话分开存，调试记录不会混进线上会话；
 * - 子表：chat_dialog 存消息（一轮两条 user / assistant），agentic_log 存每轮运行明细，
 *   两者都用 chat_id 列关联（无外键约束）；
 * - 删除为标记删除：deleted_time=0 表示未删除；删除会话时消息与运行日志一并打标记，便于追溯。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String title; // 会话标题
    @Column
    private String type; // 会话类型
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;
    @Column
    private String deletedReason; // 删除原因
    @Column
    private String deletedDetail; // 删除描述
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

}
