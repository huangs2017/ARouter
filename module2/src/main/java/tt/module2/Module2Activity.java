package tt.module2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import tt.router_annotation.Extra;
import tt.router_annotation.Route;
import androidx.annotation.Nullable;
import tt.router_core.ARouter;
import tt.router_core.template.IService;


@Route(path = "/module2/test")
public class Module2Activity extends Activity {

    @Extra
    String msg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module2);
        ARouter.getInstance().inject(this);
        Log.i("module2", "我是模块2:" + msg);

        //当处于组件模式的时候
        if (BuildConfig.isModule){
            IService testService = (IService) ARouter.getInstance().build("/main/service1").navigation();
            testService.test();
            IService testService1 = (IService) ARouter.getInstance().build("/main/service2").navigation();
            testService1.test();
            IService testService2 = (IService) ARouter.getInstance().build("/module1/service").navigation();
            testService2.test();
            IService testService3 = (IService) ARouter.getInstance().build("/module2/service").navigation();
            testService3.test();
        }
    }

    public void mainJump(View view) {
        if (BuildConfig.isModule){
            ARouter.getInstance().build("/main/test").withString("a", "从Module2").navigation(this);
        }else{
            Toast.makeText(this,"当前处于组件模式,无法使用此功能", Toast.LENGTH_SHORT).show();
        }
    }

    public void module1Jump(View view) {
        ARouter.getInstance().build("/module1/test").withString("msg", "从Module2").navigation(this);
    }
}
