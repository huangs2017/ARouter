package tt.router_compiler;

import com.google.auto.service.AutoService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import tt.router_annotation.Extra;
import tt.router_compiler.utils.Consts;
import tt.router_compiler.utils.GenerateJavaFile;
import tt.router_compiler.utils.Utils;

@AutoService(Processor.class)
@SupportedOptions(Consts.moduleName)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("tt.router_annotation.Extra")

public class ExtraProcessor extends AbstractProcessor {

    private Elements elementUtils;  // 用来处理的Element的工具
    private Types typeUtils;        // 用来处理TypeMirror的工具
    private Filer filer;            // 文件生成器

    // key:类节点  value:类中使用注解的属性集合
    private HashMap<TypeElement, List<Element>> activityMap = new HashMap<>();


    // 初始化 从 processingEnvironment 获得一系列处理器工具
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        filer = processingEnv.getFiler();
    }

    /**
     * @param set
     * @param roundEnvironment 表示当前或是之前的运行环境,可以通过该对象查找找到的注解。
     * @return true 表示后续处理器不会再处理(已经处理)
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //被注解的属性节点集合
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Extra.class);
        if (!Utils.isEmpty(elements)) {
            try {
                categories(elements);
                GenerateJavaFile.generateExtra(activityMap, elementUtils, typeUtils, filer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    // 记录需要生成的类与属性
    private void categories(Set<? extends Element> elements) {
        for (Element element : elements) {
            //获得父节点 (类)
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            if (activityMap.containsKey(enclosingElement)) {
                activityMap.get(enclosingElement).add(element);
            } else {
                List<Element> elementList = new ArrayList<>();
                elementList.add(element);
                activityMap.put(enclosingElement, elementList);
            }
        }
    }


}
