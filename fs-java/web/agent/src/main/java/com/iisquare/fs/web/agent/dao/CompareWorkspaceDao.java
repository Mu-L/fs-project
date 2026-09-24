package com.iisquare.fs.web.agent.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.agent.entity.CompareWorkspace;

import java.util.List;

/**
 * 工作区的数据访问：一个用户多份，且支持共享给所有人看。
 * 列表走轻量投影，不读 models / settings 两个大字段。可见性分两条查询：
 * 「我的」与「别人共享给我的」，由服务层合并并打上 mine 标记，页面据此区分能做什么。
 */
public interface CompareWorkspaceDao extends DaoBase<CompareWorkspace, Integer> {

    /**
     * 列表用的轻量投影：只要展示需要的小字段
     */
    interface WorkspaceIndex {
        Integer getId();
        String getName();
        Integer getModelCount();
        Integer getShared();
        Long getSharedTime();
        Long getCreatedTime();
        Long getUpdatedTime();
        Integer getCreatedUid();
    }

    /** 我的工作区（含我共享出去的），最近改动的在前 */
    List<WorkspaceIndex> findByCreatedUidAndDeletedTimeOrderByIdDesc(Integer createdUid, Long deletedTime);

    /** 别人共享出来的工作区，最近改动的在前 */
    List<WorkspaceIndex> findBySharedAndCreatedUidNotAndDeletedTimeOrderByIdDesc(
            Integer shared, Integer createdUid, Long deletedTime);

    /** 取某条工作区（不限归属）：可见性由服务层判断，否则「别人的共享工作区」看不到 */
    CompareWorkspace findFirstByIdAndDeletedTime(Integer id, Long deletedTime);

}
