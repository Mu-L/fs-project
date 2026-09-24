package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 模型对比调试的工作区：一个用户可以有多个，各自命名，用来并行推进不同场景的对比测试。
 *
 * - name 工作区名称，由用户显式保存时填写；models 选中的模型及各自的独立参数，
 *   settings 全局设置（系统提示词、用户输入、流式与思考开关、列数）；
 * - modelCount 是写入时算好的模型个数：列表只要这些摘要，不必为了显示一行去读两个 longtext；
 * - 归属看 createdUid；shared=1 表示已共享，此时所有人可见，但只有属主能改、能删、能收回共享
 *   （可见不等于可改：他人打开共享工作区后要留成自己的，走「另存为」新建一份）；
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
public class CompareWorkspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 工作区名称
    @Column
    private Integer modelCount; // 选中的模型个数（写入时算好）
    @Column
    private String models; // 选中的模型及各自的独立参数（JSON 数组）
    @Column
    private String settings; // 全局设置（JSON 对象）
    @Column
    private Integer shared; // 是否共享：0-私有（默认，仅本人可见），1-已共享（所有人可见）
    @Column
    private Long sharedTime; // 共享时间，0 表示未共享
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
