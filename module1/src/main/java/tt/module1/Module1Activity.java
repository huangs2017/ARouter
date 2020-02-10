package tt.module1;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import tt.router_annotation.Extra;
import tt.router_annotation.Route;
import tt.router_core.ARouter;
import tt.router_core.template.IService;

@Route(path = "/module1/test")
public class Module1Activity extends Activity {

    @Extra
    String msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module1);
        ARouter.getInstance().inject(this);
        Log.i("module1", "我是模块1:" + msg);

        IService testService = (IService) ARouter.getInstance().build("/main/service1").navigation();
        testService.test();
        IService testService1 = (IService) ARouter.getInstance().build("/main/service2").navigation();
        testService1.test();
        IService testService2 = (IService) ARouter.getInstance().build("/module1/service").navigation();
        testService2.test();
        IService testService3 = (IService) ARouter.getInstance().build("/module2/service").navigation();
        testService3.test();
    }

    public void mainJump(View view) {
        ARouter.getInstance().build("/main/test").withString("a", "从Module1").navigation(this);
    }

    public void module2Jump(View view) {
        ARouter.getInstance().build("/module2/test").withString("msg", "从Module1").navigation(this);
    }
}
