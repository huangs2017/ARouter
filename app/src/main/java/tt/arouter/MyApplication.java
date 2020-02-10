package tt.arouter;

import android.app.Application;

import tt.router_core.ARouter;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ARouter.init(this);
    }
}
