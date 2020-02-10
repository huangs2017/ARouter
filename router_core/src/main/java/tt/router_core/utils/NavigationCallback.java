package tt.router_core.utils;

import tt.router_core.Postcard;

public interface NavigationCallback {
    void onFound(Postcard postcard);    // 找到跳转页面
    void onLost(Postcard postcard);     // 未找到
    void onArrival(Postcard postcard);  // 成功跳转
}
