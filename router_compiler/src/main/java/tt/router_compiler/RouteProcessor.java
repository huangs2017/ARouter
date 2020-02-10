package tt.router_compiler;

import com.google.auto.service.AutoService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import tt.router_annotation.Route;
import tt.router_annotation.RouteMeta;
import tt.router_compiler.utils.Consts;
import tt.router_compiler.utils.GenerateJavaFile;
import tt.router_compiler.utils.Log;
import tt.router_compiler.utils.Utils;

/*
 * @AutoService 的作是用来生成                 META-INF/services/javax.annotation.processing.Processor 文件的，
 * 也就是我们在使用注解处理器的时候需要手动添加    META-INF/services/javax.annotation.processing.Processor，
 * 而有了@AutoService后它会自动帮我们生成。
 * AutoService是Google开发的一个库，使用时需要在 router_compiler的build.gradle中添加依赖
 */
@AutoService(Processor.class)                       // 注册注解处理器
@SupportedOptions(Consts.moduleName)            // 处理器接收的参数，替代 AbstractProcessor#getSupportedOptions() 函数
@SupportedSourceVersion(SourceVersion.RELEASE_7)    // 声明注解支持的JDK的版本，替代 AbstractProcessor#getSupportedSourceVersion() 函数
/*
 * 声明要处理哪一些注解  替代 {@link AbstractProcessor#getSupportedAnnotationTypes()} 函数
 * 该方法返回字符串的集合表示该处理器用于处理哪些注解
 */
@SupportedAnnotationTypes({"tt.router_annotation.Route"})

public class RouteProcessor extends AbstractProcessor {

    private Map<String, List<RouteMeta>> groupMap = new HashMap<>();    // 分组 key:组名 value:对应组的路由信息
    private Map<String, String> rootMap = new HashMap<>();              // key:组名 value:类名
    private Elements elementUtils;  // 用来处理的Element的工具
    private Types typeUtils;        // 用来处理TypeMirror的工具
    private Filer filer;            // 文件生成器
    private String moduleName;      // 参数
    private Log log;


    // 初始化 从 processingEnvironment 获得一系列处理器工具
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //获得apt的日志输出
        log = Log.newLog(processingEnvironment.getMessager());
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        filer = processingEnvironment.getFiler();
        //参数是模块名 为了防止多模块/组件化开发的时候 生成相同的 xx$$ROOT$$文件
        Map<String, String> options = processingEnvironment.getOptions();
        if (!Utils.isEmpty(options)) {
            moduleName = options.get(Consts.moduleName);
        }
        log.i("RouteProcessor Parameter:" + moduleName);
        if (Utils.isEmpty(moduleName)) {
            throw new RuntimeException("Not set Processor Parameter.");
        }
    }

    /**
     * 相当于main函数，正式处理注解
     *
     * @param set 使用了支持处理注解  的节点集合
     * @param roundEnvironment 表示当前或是之前的运行环境，可以通过该对象查找注解。
     * @return true 表示后续处理器不会再处理(已经处理)
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        /*
            public class className {        TypeElement         类
                public void method(){}      ExecutableElement   方法
                public int a;               VariableElement     属性
            }
        */

        //获取所有被 Route 注解的元素集合
        Set<TypeElement> routeElements = (Set<TypeElement>) roundEnvironment.getElementsAnnotatedWith(Route.class);
        //处理 Route 注解
        if (!Utils.isEmpty(routeElements)) {
            try {
                parseRoutes(routeElements);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void parseRoutes(Set<TypeElement> routeElements) throws IOException {
        //支持配置路由类的类型（获得当前节点的类型 activity这个类的 节点信息，全类名）
        /**
         * elementUtils 通过节点工具，生成节点
         */
        TypeElement activity = elementUtils.getTypeElement("android.app.Activity");
        TypeMirror tmActivity = activity.asType(); // 节点描述
        TypeElement iService = elementUtils.getTypeElement("tt.router_core.template.IService");
        TypeMirror tmIService = iService.asType();


        /**
         * groupMap(组名:路由信息)集合
         */
        //声明 Route 注解的节点 (需要处理的节点 Activity/IService)
        for (Element element : routeElements) {
            RouteMeta routeMeta;                    // 路由信息
            TypeMirror tm = element.asType();       // 使用Route注解的类信息 (如activity)
            log.i("Route Class: " + tm.toString());
            Route route = element.getAnnotation(Route.class);
            /**
             * 判断Route注解使用在什么类上面
             */
            if (typeUtils.isSubtype(tm, tmActivity)) {
                routeMeta = new RouteMeta(RouteMeta.Type.ACTIVITY, element, route);
            } else if (typeUtils.isSubtype(tm, tmIService)) {
                routeMeta = new RouteMeta(RouteMeta.Type.ISERVICE, element, route);
            } else {
                throw new RuntimeException("[Just Support Activity/IService Route] :" + element);
            }
            //分组信息记录  groupMap <Group分组,RouteMeta路由信息> 集合
            //检查是否配置 group 如果没有配置，则从path截取出组名
            categories(routeMeta);
        }

       //生成类需要实现的接口
        TypeElement iRouteGroup = elementUtils.getTypeElement("tt.router_core.template.IRouteGroup");
        log.i("---------" + iRouteGroup.getSimpleName());
        TypeElement iRouteRoot = elementUtils.getTypeElement("tt.router_core.template.IRouteRoot");

        // 生成Group类 作用: 记录 <地址，RouteMeta路由信息(Class文件等信息)>
        GenerateJavaFile.generateGroup(iRouteGroup, groupMap, rootMap, filer);
        // 生成Root类 作用: 记录 <分组，对应的Group类>
        GenerateJavaFile.generateRoot(iRouteRoot, iRouteGroup, rootMap, filer, moduleName);
    }

    private void categories(RouteMeta routeMeta) {
        if (routeVerify(routeMeta)) {
            log.i("Group Info, Group Name = " + routeMeta.getGroup() + ", Path = " + routeMeta.getPath());
            List<RouteMeta> routeMetas = groupMap.get(routeMeta.getGroup());
            //如果未记录分组则创建
            if (Utils.isEmpty(routeMetas)) {
                List<RouteMeta> routeMetaSet = new ArrayList<>();
                routeMetaSet.add(routeMeta);
                groupMap.put(routeMeta.getGroup(), routeMetaSet);
            } else {
                routeMetas.add(routeMeta);
            }
        } else {
            log.i("Group Info Error: " + routeMeta.getPath());
        }
    }

    /**
     * 验证路由信息必须存在path(并且设置分组)
     * 验证地址合法性
     * @param meta raw meta
     */
    private boolean routeVerify(RouteMeta meta) {
        String path = meta.getPath();
        String group = meta.getGroup();
        //如果没有设置分组,以第一个 / 后的节点为分组(所以必须path两个/)
        if (Utils.isEmpty(group)) {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (Utils.isEmpty(defaultGroup)) {
                return false;
            }
            meta.setGroup(defaultGroup);
            return true;
        }
        return true;
    }


}
