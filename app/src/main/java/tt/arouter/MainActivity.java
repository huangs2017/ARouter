package tt.arouter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import tt.arouter.parcelable.TestParcelable;
import tt.router_core.ARouter;
import tt.router_core.template.IService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 组件服务共享 通信
         */
        IService testService = (IService) ARouter.getInstance().build("/main/service1").navigation();
        testService.test();
        IService testService1 = (IService) ARouter.getInstance().build("/main/service2").navigation();
        testService1.test();
        IService testService2 = (IService) ARouter.getInstance().build("/module1/service").navigation();
        testService2.test();
        IService testService3 = (IService) ARouter.getInstance().build("/module2/service").navigation();
        testService3.test();
    }


    // 应用内跳转
    public void innerJump(View view) {
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(1);
        integers.add(2);

        ArrayList<String> strings = new ArrayList<>();
        strings.add("1");
        strings.add("2");

        ArrayList<TestParcelable> ps = new ArrayList<>();

        TestParcelable testParcelable = new TestParcelable(1, "a");
        TestParcelable testParcelable2 = new TestParcelable(2, "d");
        ps.add(testParcelable);
        ps.add(testParcelable2);

        ARouter.getInstance().build("/main/test")
                .withString("a", "从MainActivity")
                .withInt("b", 1)
                .withInt("hhhhhh", 1)
                .withShort("c", (short) 2)
                .withLong("d", 3)
                .withFloat("e", 1.0f)
                .withDouble("f", 1.1)
                .withByte("g", (byte) 1)
                .withBoolean("h", true)
                .withChar("i", '好')
                .withParcelable("j", testParcelable)
                .withStringArray("aa", new String[]{"1", "2"})
                .withIntArray("bb", new int[]{1, 2})
                .withShortArray("cc", new short[]{(short) 2, (short) 2})
                .withLongArray("dd", new long[]{1, 2})
                .withFloatArray("ee", new float[]{1.0f, 1.0f})
                .withDoubleArray("ff", new double[]{1.1, 1.1})
                .withByteArray("gg", new byte[]{(byte) 1, (byte) 1})
                .withBooleanArray("hh", new boolean[]{true, true})
                .withCharArray("ii", new char[]{'好', '好'})
                .withParcelableArray("jj", new TestParcelable[]{testParcelable, testParcelable2})
                .withParcelableArrayList("k1", ps)
                .withParcelableArrayList("k2", ps)
                .withStringArrayList("k3", strings)
                .withIntegerArrayList("k4", integers)
                .navigation(this, 100);
    }

    // 跳转模块1
    public void module1Jump(View view) {
        ARouter.getInstance().build("/module1/test").withString("msg", "从MainActivity").navigation();
    }

    // 跳转模块2
    public void module2Jump(View view) {
        ARouter.getInstance().build("/module2/test").withString("msg", "从MainActivity").navigation();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("Main", requestCode + ":" + resultCode + ":" + data);
    }
}
