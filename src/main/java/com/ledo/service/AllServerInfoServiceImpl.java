package com.ledo.service;

import com.ledo.beans.AllServerInfo;
import com.ledo.beans.UrlContent;
import com.ledo.dao.IAllServer;
import com.ledo.dao.IUrlContent;
import com.ledo.manager.DateManager;
import com.ledo.manager.FileManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.ledo.common.FileConstant.*;

/** 更新服务器全部信息
 * @author qgl
 * @date 2018/10/9
 */
@Service("allServerInfoService")
public class AllServerInfoServiceImpl extends BaseService implements IAllServerInfoService{

    @Autowired
    @Qualifier("IAllServer")
    private IAllServer allServerDao;

    @Autowired
    @Qualifier("IUrlContent")
    private IUrlContent urlContentDao;

    @Override
    public ArrayList<AllServerInfo> referAllServerInfo() {
        return allServerDao.queryAllServerInfo();
    }

    @Override
    public ArrayList<AllServerInfo> referServerInfoByCondition(AllServerInfo serverInfo) {
        return allServerDao.queryByCondition(serverInfo);
    }

    @Override
    public void onlyUpdateServerOpenDaysInfo() {
        HashMap<String, Integer> openDaysMap = DateManager.getInstance().getOpenDaysByServerOpenTime(referAllServerInfo());
        for (Map.Entry<String, Integer> openDaysEntry : openDaysMap.entrySet()) {
            allServerDao.updateServerOpenDaysInfo(openDaysEntry.getKey(),openDaysEntry.getValue());
        }
    }

    @Override
    public ArrayList<AllServerInfo> referLinuxServerInfo() {
        return allServerDao.queryAllServerInfo();
    }

    @Override
    public void deleteLinuxServerInfo() {
        allServerDao.deleteLinuxServerInfo();
    }

    @Override
    public void updateLinuxServerInfo() {
        HashMap<Integer, String> inlandZoneNameAndId = FileManager.getInstance().getInlandZoneIdAndName(RECHARGE_LOG_DIRECTORY);
        HashMap<Integer, String> gatZoneNameAndId = FileManager.getInstance().getGatZoneIdAndName(RECHARGE_LOG_DIRECTORY);
        HashMap<String, String> zoneOpt = FileManager.getInstance().getZoneOpt(ZONE_OPT_PATH);
        HashMap<Integer, String> daluOpenDays = FileManager.getInstance().getInlandZoneIdAndOpenTime(SERVER_OPENDAYS_PATH);
        HashMap<Integer, String> gatOpenDays = FileManager.getInstance().getGatZoneIdAndOpenTime(SERVER_OPENDAYS_PATH);
        for (UrlContent content : urlContentDao.queryUrlContents()) {
            // 更新服务器信息时 区分港澳台与大陆的差异
            if (content.getChannel().contains("GAT")) {
                addData(gatZoneNameAndId, zoneOpt, content, gatOpenDays);
            } else {
                addData(inlandZoneNameAndId, zoneOpt, content, daluOpenDays);
            }
        }
    }

    /**
     * 保存Linux服务器信息
     * @param zoneNameAndId
     * @param zoneOpt
     * @param content
     * @param serverOpenDays
     */
    public void addData(HashMap<Integer, String> zoneNameAndId, HashMap<String, String> zoneOpt, UrlContent content, HashMap<Integer, String> serverOpenDays) {
        String channel = content.getChannel();
        int zoneId = content.getZoneId();
        String serverName = content.getServerName();

        int optOrId = 0;
        String innerIp = null;
        String outerIp = null;
        String hostName = null;
        String openTime = null;
        int openDays = 0;

        if (zoneNameAndId.containsKey(zoneId)){
            hostName = zoneNameAndId.get(zoneId);
            if (serverOpenDays.containsKey(zoneId)) {
                openTime = serverOpenDays.get(zoneId);
                openDays = DateManager.getInstance().getServerOpenDays(openTime);
            }
            if (zoneOpt.containsKey(hostName)) {
                String[] idAndIp = zoneOpt.get(hostName).split("=");
                if (idAndIp.length > 2) {
                    optOrId = Integer.valueOf(idAndIp[0]);
                    innerIp = idAndIp[1];
                    outerIp = idAndIp[2];
                }
                allServerDao.insertLinuxServerInfo(new AllServerInfo(channel, zoneId, serverName, optOrId, innerIp, outerIp, hostName, openTime, openDays));
            }else {
                logger.error("更新Linux信息失败，未知zoneId:" + zoneId + " 未知hostName：" + hostName);
            }
        }else {
            logger.error("更新Linux信息失败，新开服:" + zoneId + " " + serverName);
        }
    }

}
