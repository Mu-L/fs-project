package com.iisquare.fs.web.agent.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 编排会话：一个用户针对某个编排应用的一条对话线程。
 *
 * - 会话归属编排（agenticId）与用户（createdUid），两种类型分开存：
 *   draft-设计器调试运行，published-发布应用；续写时按这三个条件校验，避免读到他人会话；
 * - leafId 是当前分支尾消息标识（fs_agent_agentic_dialog.id）：
 *   多轮上下文按它沿消息的 parentId 回溯取，而不是按 id 顺序取全量；
 * - 消息见 AgenticDialog，每轮运行明细见 AgenticLog（按 chatId 关联）；
 * - 删除为标记删除：deletedTime=0 表示未删除，deletedUid 记录删除人便于排查。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class AgenticChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer agenticId; // 所属编排
    @Column
    private String title; // 会话标题
    @Column
    private String type; // 会话类型：draft-调试运行，published-发布应用
    @Column
    private Integer leafId; // 当前分支尾消息标识
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
    private Integer deletedUid; // 删除人，0-系统标记删除

}
