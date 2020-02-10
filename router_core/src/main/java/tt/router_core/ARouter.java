package tt.router_core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import java.util.Set;
import tt.router_annotation.RouteMeta;
import tt.router_core.template.IRouteGroup;
import tt.router_core.template.IRouteRoot;
import tt.router_core.template.IService;
import tt.router_core.utils.ClassUtils;
import tt.router_core.utils.NavigationCallback;
import tt.router_core.utils.NoRouteFoundException;

public class ARouter {

    private static final String TAG = "ARouter";
    private static final String ROUTE_ROOT_PAKCAGE = "tt.router_core";
    private static final String SDK_NAME = "ARouter";
    private static final String SUFFIX_ROOT = "Root";
    private static Application mContext;
    private static ARouter instance;

    public static ARouter getInstance() {
        synchronized (ARouter.class) {
            if (instance == null) {
                instance = new ARouter();
            }
        }
        return instance;
    }

    /**
     * 初始化
     *
     * @param application
     */
    public static void init(Application application) {
        mContext = application;
        try {
            loadInfo();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "初始化失败!", e);
        }
    }


    /**
     * 分组表制作
     */
    private static void loadInfo() throws Exception {
        //获得所有 apt生成的路由类的全类名 (路由表)
        Set<String> routerMap = ClassUtils.getFileNameByPackageName(mContext, ROUTE_ROOT_PAKCAGE);
        for (String className : routerMap) {
            if (className.startsWith(ROUTE_ROOT_PAKCAGE + "." + SDK_NAME + "$$" + SUFFIX_ROOT)) {
                // root中注册的是分组信息 将分组信息加入仓库中
                IRouteRoot iRouteRoot = (IRouteRoot) (Class.forName(className).getConstructor().newInstance());
                iRouteRoot.loadInto(Warehouse.groupsIndex);
            }
        }
//        for (Map.Entry<String, Class<? extends IRouteGroup>> stringClassEntry : Warehouse.groupsIndex.entrySet()) {
//            Log.e(TAG, "Root映射表[ " + stringClassEntry.getKey() + " : " + stringClassEntry.getValue() + "]");
//        }
    }


    public Postcard build(String path) {
        String group = path.substring(1, path.indexOf("/", 1)); //获得path中的组名
        return new Postcard(path, group);
    }

    /**
     * 根据跳卡跳转页面
     *
     * @param context
     * @param postcard
     * @param requestCode
     * @param callback
     */
    protected Object navigation(Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        try {
            prepareCard(postcard);
        } catch (NoRouteFoundException e) {
            e.printStackTrace();
            //没找到
            if (null != callback) {
                callback.onLost(postcard);
            }
            return null;
        }
        if (null != callback) {
            callback.onFound(postcard);
        }

        switch (postcard.getType()) {
            case ACTIVITY:
                final Context currentContext = null == context ? mContext : context;
                //postcard.getDestination = Module1Activity.class
                final Intent intent = new Intent(currentContext, postcard.getDestination());
                intent.putExtras(postcard.getExtras());
                int flags = postcard.getFlags();
                if (-1 != flags) {
                    intent.setFlags(flags);
                } else if (!(currentContext instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //可能需要返回码
                        if (requestCode > 0) {
                            ActivityCompat.startActivityForResult((Activity) currentContext, intent, requestCode, postcard.getOptionsBundle());
                        } else {
                            ActivityCompat.startActivity(currentContext, intent, postcard.getOptionsBundle());
                        }

                        if ((0 != postcard.getEnterAnim() || 0 != postcard.getExitAnim()) && currentContext instanceof Activity) {
                            //老版本
                            ((Activity) currentContext).overridePendingTransition(postcard.getEnterAnim(), postcard.getExitAnim());
                        }
                        //跳转完成
                        if (null != callback) {
                            callback.onArrival(postcard);
                        }
                    }
                });
                break;
            case ISERVICE:
                return postcard.getService();
            default:
                break;
        }
        return null;
    }

    // 准备卡片
    private void prepareCard(Postcard card) {
        RouteMeta routeMeta = Warehouse.routes.get(card.getPath());
        //还没准备的
        if (null == routeMeta) {
            //创建并调用 loadInto 函数,然后记录在仓库
            Class<? extends IRouteGroup> groupMeta = Warehouse.groupsIndex.get(card.getGroup());
            IRouteGroup iGroupInstance;
            try {
                iGroupInstance = groupMeta.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("路由分组映射表记录失败.", e);
            }
            iGroupInstance.loadInto(Warehouse.routes);
            //已经准备过了就可以移除了 (不会一直存在内存中)
            Warehouse.groupsIndex.remove(card.getGroup());
            //再次进入 else
            prepareCard(card);
        } else {
            //类 要跳转的activity 或IService实现类
            card.setDestination(routeMeta.getDestination());
            card.setType(routeMeta.getType());
            switch (routeMeta.getType()) {
                case ISERVICE:
                    //TestServiceImpl
                    Class<?> destination = routeMeta.getDestination();
                    IService service = Warehouse.services.get(destination);
                    if (null == service) {
                        try {
                            service = (IService) destination.getConstructor().newInstance();
                            Warehouse.services.put(destination, service);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    card.setService(service);
                    break;
                default:
                    break;
            }
        }
    }


    // 注入
    public void inject(Activity instance) {
        ExtraManager.getInstance().loadExtras(instance);
    }

}
