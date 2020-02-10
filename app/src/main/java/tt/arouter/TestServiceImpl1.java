package tt.arouter;

import android.util.Log;
import tt.router_annotation.Route;
import tt.router_core.template.IService;

@Route(path = "/main/service1")
public class TestServiceImpl1 implements IService {
    @Override
    public void test() {
        Log.i("Service", "我是app模块测试服务通信1");
    }
}
