package tt.module1;

import android.util.Log;
import tt.router_annotation.Route;
import tt.router_core.template.IService;

@Route(path = "/module1/service")
public class TestServiceImpl implements IService {
    @Override
    public void test() {
        Log.i("Service", "我是Module1模块测试服务通信");
    }
}
