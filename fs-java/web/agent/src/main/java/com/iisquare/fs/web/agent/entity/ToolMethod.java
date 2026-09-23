package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 工具方法：工具对外暴露的可调用方法，由工具配置（OpenAPI 文档 / MCP 同步结果）解析而来。
 * 业务唯一键为 toolId + name，重解析按该唯一键 upsert，present 标记本次解析是否仍存在。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class ToolMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer toolId; // 所属工具
    @Column
    private String name; // 方法名：给模型与调用使用，已规范化为合法函数名
    @Column
    private String originName; // 原始方法名：OpenAPI 的 operationId、MCP 的 tool name
    @Column
    private String title; // 展示名称
    @Column
    private String description; // 方法描述
    @Column
    private String params; // 方法级参数：[{ name, type, required, description, in, defaultValue, enum }]
    @Column
    private String invoke; // 调用信息：schema 为 { server, method, path }，mcp 为 { name }
    @Column
    private Integer sort;
    @Column
    private Integer status; // 1-启用，2-停用
    @Column
    private Integer present; // 1-最近一次解析存在，0-已失效
    @Column
    private String parseError; // 解析告警
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
