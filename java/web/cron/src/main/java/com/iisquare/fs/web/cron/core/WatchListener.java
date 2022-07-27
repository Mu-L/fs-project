package com.iisquare.fs.web.cron.core;

import com.iisquare.fs.web.cron.service.NodeService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

import java.util.Arrays;

public class WatchListener implements TreeCacheListener {

    public static final String CMD_EMPTY = "";
    public static final String CMD_ERROR = "ERROR";
    public static final String CMD_STANDBY = "WAIT_STANDBY";
    public static final String CMD_DONE_STANDBY = "DONE_STANDBY";
    public static final String CMD_RESTART = "WAIT_RESTART";
    public static final String CMD_FORCE_RESTART = "WAIT_FORCE_RESTART";
    public static final String CMD_DONE_RESTART = "DONE_RESTART";
    public static final String CMD_SHUTDOWN = "WAIT_SHUTDOWN";
    public static final String CMD_FORCE_SHUTDOWN = "WAIT_FORCE_SHUTDOWN";
    public static final String CMD_DONE_SHUTDOWN = "DONE_SHUTDOWN";

    private NodeService nodeService;

    public WatchListener(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public static boolean canStart(String command) {
        return Arrays.asList(CMD_EMPTY, CMD_RESTART, CMD_FORCE_RESTART, CMD_DONE_RESTART).contains(command);
    }

    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        ChildData data = event.getData();
        if (null == data) return;
        String path = data.getPath();
        if (path.equals("/runtime/notice")) {
            byte[] message = data.getData();
            nodeService.onNotice(new String(message));
            return;
        }
        if (event.getType().equals(TreeCacheEvent.Type.NODE_UPDATED) && path.startsWith("/runtime/command/")) {
            String nodeId = path.substring("/runtime/command/".length());
            ZooKeeperClient zookeeper = nodeService.zookeeper();
            if (!nodeId.equals(zookeeper.nodeId())) return;
            byte[] message = data.getData();
            if (null == message) return;
            String command = new String(message, ZooKeeperClient.charset);
            switch (command) {
                case CMD_STANDBY:
                    nodeService.standby();
                    zookeeper.command(CMD_DONE_STANDBY);
                    break;
                case CMD_RESTART:
                    nodeService.restart(true);
                    zookeeper.command(CMD_DONE_RESTART);
                    break;
                case CMD_FORCE_RESTART:
                    nodeService.restart(false);
                    zookeeper.command(CMD_DONE_RESTART);
                    break;
                case CMD_SHUTDOWN:
                    nodeService.shutdown(true);
                    zookeeper.command(CMD_DONE_SHUTDOWN);
                    break;
                case CMD_FORCE_SHUTDOWN:
                    nodeService.shutdown(false);
                    zookeeper.command(CMD_DONE_SHUTDOWN);
                    break;
            }
        }
    }
}
