package com.iisquare.fs.web.agent.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.agent.entity.CompareRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 运行记录的数据访问：列表走轻量投影，不把 settings / models 两个大字段读出来
 * （各模型的输出已经拆到 CompareCall，这里更没有整行读的必要）。
 */
public interface CompareRunDao extends DaoBase<CompareRun, Integer> {

    /**
     * 列表用的轻量投影：只要展示与筛选需要的小字段
     */
    interface RunIndex {
        Integer getId();
        String getTitle();
        Integer getStatus();
        Integer getModelCount();
        Long getDuration();
        String getSummary();
        Long getCreatedTime();
        Integer getCreatedUid();
    }

    /**
     * 我的记录，按标题检索、按 id 倒序分页。
     * 标题里存的是用户输入，所以检索输入内容就是查标题。
     * 不传关键词时由服务层给 `%%`，即「全都匹配」，省得多维护一条无筛选的查询。
     */
    @Query("select r.id as id, r.title as title, r.status as status, r.modelCount as modelCount, "
            + "r.duration as duration, r.summary as summary, r.createdTime as createdTime, r.createdUid as createdUid "
            + "from CompareRun r "
            + "where r.createdUid = :uid and r.deletedTime = 0 and r.title like concat('%', :title, '%') "
            + "order by r.id desc")
    Page<RunIndex> search(@Param("uid") Integer uid, @Param("title") String title, Pageable pageable);

}
