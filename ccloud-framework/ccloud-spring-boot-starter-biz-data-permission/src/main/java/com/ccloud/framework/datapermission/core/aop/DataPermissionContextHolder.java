package com.ccloud.framework.datapermission.core.aop;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.ccloud.framework.datapermission.core.annotation.DataPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限上下文。用栈结构保存，支持嵌套调用时正确回退到外层设置。
 */
public class DataPermissionContextHolder {

    private static final ThreadLocal<List<DataPermission>> DATA_PERMISSIONS =
            TransmittableThreadLocal.withInitial(ArrayList::new);

    /**
     * 取当前生效的注解。
     *
     * <p>规则是<b>关闭优先</b>：栈中只要有任意一层显式关闭了数据权限，就以关闭为准。
     * {@code DataPermissionUtils.executeIgnore} 是跨部门统计、系统任务这类操作的
     * 显式逃生口，如果内层 Service 方法上的 {@code @DataPermission} 能把它重新打开，
     * 它就包不住任何带注解的方法，等于形同虚设。
     *
     * @return 当前生效的注解；栈为空时返回 null，表示不做数据权限改写
     */
    public static DataPermission get() {
        List<DataPermission> list = DATA_PERMISSIONS.get();
        if (list.isEmpty()) {
            return null;
        }
        for (DataPermission dataPermission : list) {
            if (!dataPermission.enable()) {
                return dataPermission;
            }
        }
        return list.get(list.size() - 1);
    }

    public static void add(DataPermission dataPermission) {
        DATA_PERMISSIONS.get().add(dataPermission);
    }

    public static void remove() {
        List<DataPermission> list = DATA_PERMISSIONS.get();
        if (!list.isEmpty()) {
            list.remove(list.size() - 1);
        }
        if (list.isEmpty()) {
            DATA_PERMISSIONS.remove();
        }
    }

    public static void clear() {
        DATA_PERMISSIONS.remove();
    }
}
