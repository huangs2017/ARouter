package tt.router_compiler.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import tt.router_annotation.RouteMeta;
import static javax.lang.model.element.Modifier.PUBLIC;

public class GenerateJavaFile {

    public static void generateGroup(TypeElement iRouteGroup, Map<String, List<RouteMeta>> groupMap,
                                     Map<String, String> rootMap, Filer filer) throws IOException {
        //创建参数类型  Map<String, RouteMeta>
        ParameterizedTypeName atlas = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouteMeta.class)
        );
        //创建参数  Map<String, RouteMeta> atlas
        ParameterSpec parameterSpec = ParameterSpec.builder(atlas, "atlas").build();

        //遍历分组, 每一个分组创建一个 $$Group$$ 类
        for (Map.Entry<String, List<RouteMeta>> entry : groupMap.entrySet()) {
            /**
             * 类成员函数loadInfo声明构建
             */
            //方法 public void loadInfo(Map<String,RouteMeta> atlas)
            MethodSpec.Builder builder = MethodSpec.methodBuilder("loadInto")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .returns(TypeName.VOID)  //可以不写
                    .addParameter(parameterSpec);

            //分组名 与 对应分组中的信息
            String groupName = entry.getKey();
            List<RouteMeta> groupData = entry.getValue();
            //遍历分组中的条目 数据
            for (RouteMeta routeMeta : groupData) {
                /**
                 * $S --> String
                 * $T --> Class类
                 * $L --> 字面量
                 */
                // $S https://github.com/square/javapoet#s-for-strings
                // $T https://github.com/square/javapoet#t-for-types

                // 组装方法体:
                // atlas.put("/main/test", RouteMeta.build(RouteMeta.Type.ACTIVITY, SecondActivity.class, "/main/test", "main"));
                builder.addStatement(
                        "atlas.put($S, $T.build($T.$L, $T.class, $S, $S))",
                        routeMeta.getPath(),
                        ClassName.get(RouteMeta.class),
                        ClassName.get(RouteMeta.Type.class),
                        routeMeta.getType(),
                        ClassName.get((TypeElement) routeMeta.getElement()),
                        routeMeta.getPath().toLowerCase(),
                        routeMeta.getGroup().toLowerCase()
                );
            }

            //生成 $$Group$$类
            String groupClassName = Consts.groupName + groupName;
            TypeSpec ARouter$$Group$$main = TypeSpec.classBuilder(groupClassName)
                    .addSuperinterface(ClassName.get(iRouteGroup))
                    .addModifiers(PUBLIC)
                    .addMethod(builder.build())
                    .build();

            // 创建java文件($$Group$$)
            JavaFile javaFile = JavaFile.builder(Consts.packageOfGenerateFile, ARouter$$Group$$main).build();
            javaFile.writeTo(filer);
            //分组名和生成的对应的Group类类名
            rootMap.put(groupName, groupClassName);
        }
    }



    public static void generateRoot(TypeElement iRouteRoot, TypeElement iRouteGroup, Map<String, String> rootMap,
                                    Filer filer, String moduleName) throws IOException {
        //创建参数类型  Map<String, Class<? extends IRouteGroup>>
        //Wildcard 通配符
        ParameterizedTypeName routes = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(iRouteGroup))
                )
        );

        //创建参数  Map<String, Class<? extends IRouteGroup>> routes
        ParameterSpec parameterSpec = ParameterSpec.builder(routes, "routes").build();

        //方法 public void loadInfo(Map<String, Class<? extends IRouteGroup>> routes)
        MethodSpec.Builder builder = MethodSpec.methodBuilder("loadInto")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(parameterSpec);

        // 组装方法体:
        // routes.put("main", ARouter$$Group$$main.class);
        for (Map.Entry<String, String> entry : rootMap.entrySet()) {
            builder.addStatement(
                    "routes.put($S, $T.class)",
                    entry.getKey(),
                    ClassName.get(Consts.packageOfGenerateFile, entry.getValue())
            );
        }

        //生成 $$Root$$类
        String rootClassName = Consts.rootName + moduleName;
        TypeSpec ARouter$$Root$$app = TypeSpec.classBuilder(rootClassName)
                .addSuperinterface(ClassName.get(iRouteRoot))
                .addModifiers(PUBLIC)
                .addMethod(builder.build())
                .build();

        // 创建java文件($$Root$$)
        JavaFile javaFile = JavaFile.builder(Consts.packageOfGenerateFile, ARouter$$Root$$app).build();
        javaFile.writeTo(filer);
    }






    public static void generateExtra(Map<TypeElement, List<Element>> groupMap, Elements elementUtils, Types typeUtils, Filer filer) throws IOException {
        TypeElement IExtra = elementUtils.getTypeElement("tt.router_core.template.IExtra");
        // 创建参数  Object target
        ParameterSpec objectParamSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();
        // 遍历所有需要注入的 类:属性
        for (Map.Entry<TypeElement, List<Element>> entry : groupMap.entrySet()) {
            // 类
            TypeElement typeElement = entry.getKey();
            //封装的函数生成类
            LoadExtraBuilder loadExtra = new LoadExtraBuilder(objectParamSpec);
            loadExtra.setElementUtils(elementUtils);
            loadExtra.setTypeUtils(typeUtils);
            ClassName className = ClassName.get(typeElement);
            loadExtra.injectTarget(className);
            //遍历属性
            for (int i = 0; i < entry.getValue().size(); i++) {
                Element element = entry.getValue().get(i);
                loadExtra.buildStatement(element);
            }

            // 生成java类名
            String extraClassName = typeElement.getSimpleName() + Consts.extraName;
            // 生成 XX$$Extra
            TypeSpec SecondActivity$$Extra = TypeSpec.classBuilder(extraClassName)
                    .addSuperinterface(ClassName.get(IExtra))
                    .addModifiers(PUBLIC)
                    .addMethod(loadExtra.build())
                    .build();

            // 创建java文件($$Root$$)
            JavaFile javaFile = JavaFile.builder(className.packageName(), SecondActivity$$Extra).build();
            javaFile.writeTo(filer);
        }
    }

}
