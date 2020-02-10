package tt.router_core;

import java.util.HashMap;
import java.util.Map;
import tt.router_annotation.RouteMeta;
import tt.router_core.template.IRouteGroup;
import tt.router_core.template.IService;

public class Warehouse {
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();    // root  映射表 保存分组信息
    static Map<String, RouteMeta> routes = new HashMap<>();                            // group 映射表 保存组中的所有数据
    static Map<Class, IService> services = new HashMap<>();                            // group 映射表 保存组中的所有数据
}
