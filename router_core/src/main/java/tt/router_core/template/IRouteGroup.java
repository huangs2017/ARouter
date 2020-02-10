package tt.router_core.template;

import java.util.Map;

import tt.router_annotation.RouteMeta;

public interface IRouteGroup {
    void loadInto(Map<String, RouteMeta> atlas);
}
