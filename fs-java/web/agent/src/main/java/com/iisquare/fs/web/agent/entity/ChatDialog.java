package com.iisquare.fs.web.agent.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 会话消息：一次运行落两条 —— 用户提问（role=user）与助手回复（role=assistant）。
 *
 * - 只存「展示与多轮记忆」需要的字段：content 正文、reasoningContent 思考过程、
 *   reference 参考数据（图表等）、feedback_* 点赞点踩；历史会话打开即按这些字段还原；
 * - 运行明细（入参、节点与工具调用过程）单独存在 agentic_log，按 agentic_log.answer_id
 *   关联到本轮的助手回复，前端展开「执行过程」时才按 logId 懒加载，避免列表接口读大字段；
 * - parentId 用于「重新生成」时指向被替换的消息；
 * - 删除为标记删除：deletedTime=0 表示未删除，删除会话时随会话一并打标记。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class ChatDialog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer chatId;
    @Column
    private Integer parentId;
    @Column
    private String role; // 角色
    @Column
    private String reasoningContent; // 思考
    @Column
    private String content; // 内容
    @Column
    private String intent; // 意图识别结果
    @Column
    private String reference; // 参考数据
    @Column
    private String finishReason;
    @Column
    private String feedbackEmotion; // 反馈情绪，positive-赞，negative-踩，cancel-取消
    @Column
    private String feedbackTag; // 反馈标签
    @Column
    private String feedbackContent; // 反馈内容
    @Column
    private Long feedbackTime; // 反馈时间，最后更新为准
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;

    @Column
    private String auditReason; // 审核原因
    @Column
    private String auditDetail; // 审核描述
    @Column
    private Long auditTime; // 审核时间
    @Column
    private Integer auditUid; // 审核人员


    @Column
    private String deletedReason; // 删除原因，同步删除所属会话
    @Column
    private String deletedDetail; // 删除描述
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid; // 用户自主删除，0-系统标记删除，人工审核删除

}
