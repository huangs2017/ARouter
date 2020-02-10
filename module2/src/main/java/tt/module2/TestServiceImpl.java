package tt.module2;

import android.util.Log;
import tt.router_annotation.Route;
import tt.router_core.template.IService;

@Route(path = "/module2/service")
public class TestServiceImpl implements IService {
    @Override
    public void test() {
        Log.i("Service", "我是Module2模块测试服务通信");
    }
}

