package tt.module2;

import android.app.Application;
import tt.router_core.ARouter;

public class Module2Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ARouter.init(this);
    }

}
