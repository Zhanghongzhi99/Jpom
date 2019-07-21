package cn.keepbx.jpom.service.monitor;

import cn.hutool.core.date.DateUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.MonitorModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.monitor.Monitor;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控管理Service
 *
 * @author Arno
 */
@Service
public class MonitorService extends BaseOperService<MonitorModel> {


    @Override
    public List<MonitorModel> list() {
        JSONObject jsonObject = getJSONObject(ServerConfigBean.MONITOR_FILE);
        if (jsonObject == null) {
            return null;
        }
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(MonitorModel.class);
    }

    @Override
    public MonitorModel getItem(String id) {
        return getJsonObjectById(ServerConfigBean.MONITOR_FILE, id, MonitorModel.class);
    }

    @Override
    public void addItem(MonitorModel monitorModel) {
        saveJson(ServerConfigBean.MONITOR_FILE, monitorModel.toJson());
        //
        if (monitorModel.isStatus()) {
            Monitor.start();
        }
    }

    @Override
    public void deleteItem(String id) {
        deleteJson(ServerConfigBean.MONITOR_FILE, id);
        this.checkCronStatus();
    }

    public boolean checkCronStatus() {
        // 关闭监听
        List<MonitorModel> list = list();
        if (list == null || list.isEmpty()) {
            Monitor.stop();
            return false;
        } else {
            boolean stop = true;
            for (MonitorModel monitorModel : list) {
                if (monitorModel.isStatus()) {
                    Monitor.start();
                    stop = false;
                    break;
                }
            }
            if (stop) {
                Monitor.stop();
                return false;
            }
            return true;
        }
    }

    @Override
    public boolean updateItem(MonitorModel monitorModel) {
        try {
            monitorModel.setModifyTime(DateUtil.date().getTime());
            updateJson(ServerConfigBean.MONITOR_FILE, monitorModel.toJson());
            return true;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        } finally {
            this.checkCronStatus();
        }
        return false;
    }

    /**
     * 根据周期获取list
     *
     * @param cycle 周期
     * @return list
     */
    public List<MonitorModel> listRunByCycle(MonitorModel.Cycle cycle) {
        List<MonitorModel> list = this.list();
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .filter(monitorModel -> monitorModel.getCycle() == cycle.getCode() && monitorModel.isStatus())
                .collect(Collectors.toList());
    }

    /**
     * 设置报警状态
     *
     * @param id    监控id
     * @param alarm 状态
     */
    public synchronized void setAlarm(String id, boolean alarm) {
        MonitorModel monitorModel = getItem(id);
        if (monitorModel != null) {
            monitorModel.setAlarm(alarm);
            updateItem(monitorModel);
        }
    }
}