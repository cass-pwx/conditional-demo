# 1、**概述**

**条件装配**是`Spring Boot`一大特点，根据是否满足指定的条件来决定是否装配 Bean ，做到了动态灵活性，starter的自动配置类中就是使用@Conditional及其衍生扩展注解

上一篇文章[@SpringBootApplication使用及原理详解](https://blog.csdn.net/qq_43602877/article/details/135700676)中我们也提到了**AutoConfigurationImportSelector**最终其实就是加载了`META-INF/spring.factories`目录下的自动配置类，那这一次我们就来看看动配置类的条件装配注解



# **2、@Conditional**



## **2.1、是什么**

@Conditional：该注解是在spring4中新加的，其作用顾名思义就是按照一定的条件进行判断，满足条件才将bean注入到容器中，注解源码如下：

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

	/**
	 * All {@link Condition} classes that must {@linkplain Condition#matches match}
	 * in order for the component to be registered.
	 */
	Class<? extends Condition>[] value();

}
```

从注释我们可以看到，只有在所有指定条件匹配时才有资格注册

从代码中可知，该注解可作用在类，方法上，同时只有一个属性value，是一个Class数组，并且需要继承或者实现`Condition`接口：

我们再看看`Condition`接口

```java
@FunctionalInterface
public interface Condition {

	/**
	 * Determine if the condition matches.
	 * @param context the condition context
	 * @param metadata the metadata of the {@link org.springframework.core.type.AnnotationMetadata class}
	 * or {@link org.springframework.core.type.MethodMetadata method} being checked
	 * @return {@code true} if the condition matches and the component can be registered,
	 * or {@code false} to veto the annotated component's registration
	 */
	boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);

}
```

通过`@FunctionalInterface`我们可以看到，这是一个函数式接口，即可以使用函数式编程，lambda表达式。

再通过前面的`Conditional`解释，我们可以总结出：

- `Condition`是个接口，需要实现`matches`方法，**返回true则注入bean，false则不注入**。
- `@Conditional`通过传入一个或者多个实现了`Condition`并重写了`matches`的类，然后判断是否进行注册。

有的小伙伴可能还不是很清晰，我们接下来举一个例子来看看。



## **2.2、举例**

我们先随便建一个model类

```java
import lombok.Builder;
import lombok.Data;

/**
 * @author pengweixin
 */
@Data
@Builder
public class Language {

    private Long id;

    private String content;
}
```

然后建两个实现了Condition的类

```java
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Objects;

/**
 * @author pengweixin
 */
public class ChineseCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        String property = environment.getProperty("lang");
        return Objects.equals(property, "zh_CN");
    }
}
```

```java
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Objects;

/**
 * @author pengweixin
 */
public class EnglishCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        String property = environment.getProperty("lang");
        return Objects.equals(property, "en_US");
    }
}
```

然后接下来建一个配置类

```java
import com.pwx.model.Language;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author pengweixin
 */
@Configuration
public class MyConfig {
    
    @Bean
    @Conditional(ChineseCondition.class)
    public Language chinese() {
        
        return Language.builder().id(1L).content("华流才是最屌的").build();
    }

    @Bean
    @Conditional(EnglishCondition.class)
    public Language english() {
      
        return Language.builder().id(2L).content("english is good").build();
    }



    public static void main(String[] args) {
        System.setProperty("lang", "zh_CN");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MyConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
```

代码比较简单，就不进行解释了，我们直接看结果

![image-20240119174412508](.\conditional.assets\image-20240119174412508.png)

这说明根据条件匹配到`ChineseCondition`返回true，成功注入bean



## **2.3、@Conditional 衍生注解**

@Conditional 衍生注解被定义在了spring-boot-autoconfigure包中

![image-20240122111929394](.\conditional.assets\image-20240122111929394.png)

这些注解各位不用现在记住，我们后面拿几个看看，看懂了记起来就没那么难了。



## **2.4、@ConditionalOnBean**

这个注解，我们从名字上就能够很清楚的看出，注入的条件是Bean。也就是说，如果Bean存在，就注入，不存在就不注入。这也是一个比较重要的注解，我们看看代码

注意：@ConditionalOnBean 是匹配目前为止由应用程序上下文处理过的**bean定义**，而不是 bean实例

```java
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnBean {

	Class<?>[] value() default {};

	String[] type() default {};

	Class<? extends Annotation>[] annotation() default {};

	String[] name() default {};

	SearchStrategy search() default SearchStrategy.ALL;

	Class<?>[] parameterizedContainer() default {};

}
```

这里平时用的比较多的有value、type、name三个，这三个可以看到都是数组，也就是说可以配置多个。



### **2.4.1、怎么用**

![image-20240122113744892](.\conditional.assets\image-20240122113744892.png)

我们可以从这段注释中看到，@ConditionalOnBean注解使用在@Bean标识的方法上，都知道@Bean注解的作用是向容器中注入一个bean，也就是@ConditionalOnBean作用的时机是在生成bean的时候。

同时，从上面这段话中，我们也可以看到，@ConditionalOnBean的使用是具有先后顺序的

我们通过一个例子具体看看。

1、创建两个model类

```java
@Data
@AllArgsConstructor
public class School {

	private String name;
}

@Data
@AllArgsConstructor
public class Student {

	private String name;
}
```

2、创建一个config类

```java
@Configuration
public class BeanConfiguration {
    
    @Bean
    public School school() {
        return new School("清华大学");
    }

	@ConditionalOnBean(School.class)
    @Bean("zhangsan")
    public Student student() {
        return new Student("张三");
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanConfiguration.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
```

运行结果：

![image-20240122141901559](.\conditional.assets\image-20240122141901559.png)

如果换一个写法，把`school()`方法和`student()`方法互换位置。

```java
@Configuration
public class BeanConfiguration {

	@ConditionalOnBean(School.class)
    @Bean("zhangsan")
    public Student student() {
        return new Student("张三");
    }

    @Bean
    public School school() {
        return new School("清华大学");
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanConfiguration.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
```

运行结果如下：

![image-20240122142139319](.\conditional.assets\image-20240122142139319.png)

可以看到，`student`类没有被初始化了。这也满足我们之前说的，@ConditionalOnBean的使用是具有先后顺序的

对于type属性，**type要求填入的是类的全路径，比如`com.pwx.model.School`**

对于name属性，**name要求填入的是类名，比如`school`**

对于这两个属性，同样满足`@ConditionalOnBean`需要按照`@Bean`的顺序的要求。



### **2.4.2、源码解析**

我们可以看到核心就是@Conditional(OnBeanCondition.class)，点进去到`OnBeanCondition`类中

```java
class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {
    //.....
}
```

可以看到这个类继承了FilteringSpringBootCondition，点进去看看

```java
abstract class FilteringSpringBootCondition extends SpringBootCondition
		implements AutoConfigurationImportFilter, BeanFactoryAware, BeanClassLoaderAware {
    	//.....
}
```

可以看到，他继承了SpringBootCondition

```java
public abstract class SpringBootCondition implements Condition {
    // ....
}
```

可以看到，他实现了了Condition，这个我们知道，我们看看具体的`matches()`方法实现：

```java
public abstract class SpringBootCondition implements Condition {
	@Override
	public final boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    	//获取当前注解标记的类名或者方法名（由标注的位置决定）
		String classOrMethodName = getClassOrMethodName(metadata);
		try {
             //关键代码：这里就会判断出结果
			ConditionOutcome outcome = getMatchOutcome(context, metadata);
             //存入日志
			logOutcome(classOrMethodName, outcome);
             //存入日志
			recordEvaluation(context, classOrMethodName, outcome);
             //最后返回ConditionOutcome的isMatch就是返回boolean类型结果
			return outcome.isMatch();
		}
		catch (NoClassDefFoundError ex) {
			throw new IllegalStateException("Could not evaluate condition on " + classOrMethodName + " due to "
					+ ex.getMessage() + " not found. Make sure your own configuration does not rely on "
					+ "that class. This can also happen if you are "
					+ "@ComponentScanning a springframework package (e.g. if you "
					+ "put a @ComponentScan in the default package by mistake)", ex);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Error processing condition on " + getName(metadata), ex);
		}
	}
}
```

然后来到了我们的OnBeanCondition.getMatchOutcome()方法

```java
class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {
    @Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage matchMessage = ConditionMessage.empty();
		MergedAnnotations annotations = metadata.getAnnotations();
         // ConditionalOnBean 注解处理
		if (annotations.isPresent(ConditionalOnBean.class)) {
			Spec<ConditionalOnBean> spec = new Spec<>(context, metadata, annotations, ConditionalOnBean.class);
			MatchResult matchResult = getMatchingBeans(context, spec);
			if (!matchResult.isAllMatched()) {
				String reason = createOnBeanNoMatchReason(matchResult);
				return ConditionOutcome.noMatch(spec.message().because(reason));
			}
			matchMessage = spec.message(matchMessage).found("bean", "beans").items(Style.QUOTE,
					matchResult.getNamesOfAllMatches());
		}
         // ConditionalOnSingleCandidate 注解处理
         // ConditionalOnMissingBean 注解处理
		return ConditionOutcome.match(matchMessage);
	}
}

```

后面那两个部分我们后面再看，先看看ConditionalOnBean 注解处理

1. 首先，判断是不是存在这个ConditionalOnBean.class注解的标识，这个我们的`student`类是使用了该注解，所以这个判断返回`true`

2. 生成一个spec对象，该类是从底层的注解中提取的搜索规范；

   ![image-20240122150820353](.\conditional.assets\image-20240122150820353.png)

3. 进入getMatchingBeans(context, spec)方法

   ![image-20240122151940155](.\conditional.assets\image-20240122151940155.png)

   可以看得出来，就是封装了一个MatchResult对象。

    1. 从上下文【`context`】中获取 `ClassLoader` 和 `ConfigurableListableBeanFactory` ；

        - `ClassLoader` 是 **Java** 中的一个接口，用于加载类。它是 **Java** 类加载机制的核心部分，负责将 **.class** 文件转换为 **Java** 类实例。

        - `ClassLoader` 可以从不同的来源（如文件系统、网络、数据库等）加载类，也可以实现自定义的类加载逻辑。

        - ConfigurableListableBeanFactory 是 Spring 框架中的一个核心接口，它扩展了ListableBeanFactory 接口，提供了更多的配置和扩展功能。

        - 它是一个 bean 工厂的抽象概念，用于管理 Spring 容器中的 bean 对象。ConfigurableListableBeanFactory 提供了添加、移除、注册和查找 bean 的方法，以及设置和获取 bean 属性值的功能。它还支持bean 的后处理和事件传播。

    2. 这里根据 Spec 对象的 SearchStrategy 属性来确定是否考虑 bean 的层次结构。如果SearchStrategy 是 CURRENT，则不考虑层次结构【即 considerHierarchy 为 false】；否则，考虑层次结构【即 considerHierarchy 为 true】。

        - debug中我们可以看到，这里我们的strategy是SearchStrategy.ALL，所以这里是返回true

        - 这里默认都是SearchStrategy.ALL

          ```java
          public @interface ConditionalOnBean {
              SearchStrategy search() default SearchStrategy.ALL;
          }
          ```

    3. 获取 `Spec` 对象的 `parameterizedContainers` 属性，这是一个包含参数化容器类型的集合

    4. 判断Spec` 对象的 `SearchStrategy` 属性是 `SearchStrategy.ANCESTORS，调用 `getParentBeanFactory` 方法获取其父工厂，并将其转换为 `ConfigurableListableBeanFactory` 类型。当然，我们这里并不会进入这个判断。

    5. 新建一个 `MatchResult` 对象，用于存储匹配结果；

    6. 调用 `getNamesOfBeansIgnoredByType` 方法，获取被忽略类型的 `bean` 名称集合 `beansIgnoredByType` ；

        - 这一段@ConditionalOnBean并不需要做相应的判断，是在@ConditionalOnMissingBean中使用的，所以这里跳过

    7. 接下来就是进行匹配，并封装了一个MatchResult对象

4. 如果都没匹配上，那就结束了。

   ```java
   if (!matchResult.isAllMatched()) {
   	String reason = createOnBeanNoMatchReason(matchResult);
   	return ConditionOutcome.noMatch(spec.message().because(reason));
   }
   
   public static ConditionOutcome noMatch(ConditionMessage message) {
   	return new ConditionOutcome(false, message);
   }
   ```

5. 匹配上了，就封装一个匹配信息，然后返回true，完成注入

   ![image-20240122152442638](.\conditional.assets\image-20240122152442638.png)

## 2.5、**@ConditionalOnMissingBean**

看完了@ConditionalOnBean，@ConditionalOnMissingBean其实也是一样的

```java
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnMissingBean {

	Class<?>[] value() default {};

	String[] type() default {};

	Class<?>[] ignored() default {};

	String[] ignoredType() default {};

	Class<? extends Annotation>[] annotation() default {};

	String[] name() default {};
    
	SearchStrategy search() default SearchStrategy.ALL;

	Class<?>[] parameterizedContainer() default {};

}
```

### **2.5.1、怎么用**

先跑个例子看看看

```java
@Configuration
public class BeanConfig {

    @Bean(name = "notebookPC")
    public Computer computer1(){
        return new Computer("笔记本电脑");
    }

    @ConditionalOnMissingBean(Computer.class)
    @Bean("reservePC")
    public Computer computer2(){
        return new Computer("备用电脑");
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
```

运行结果：

![image-20240122170032581](.\conditional.assets\image-20240122170032581.png)

稍微修改一下：

```java
@Configuration
public class BeanConfig {

/*    @Bean(name = "notebookPC")
    public Computer computer1(){
        return new Computer("笔记本电脑");
    }*/

    @ConditionalOnMissingBean(Computer.class)
    @Bean("reservePC")
    public Computer computer2(){
        return new Computer("备用电脑");
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
```

运行结果如下：

![image-20240122170133655](.\conditional.assets\image-20240122170133655.png)

很清楚的可以看出，`@ConditionalOnMissingBean`和`@ConditionalOnBean`的作用是相反的

### **2.5.2、源码解析**

之前我们看`@ConditionalOnBean`的时候，我们看到，其实`@ConditionalOnMissingBean`的源码和`@ConditionalOnBean`就只在OnBeanCondition.getMatchOutcome()方法不一样

```java
class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConditionMessage matchMessage = ConditionMessage.empty();
        MergedAnnotations annotations = metadata.getAnnotations();
        // ConditionalOnBean 注解处理
        if (annotations.isPresent(ConditionalOnBean.class)) {
            Spec<ConditionalOnBean> spec = new Spec<>(context, metadata, annotations, ConditionalOnBean.class);
            MatchResult matchResult = getMatchingBeans(context, spec);
            if (!matchResult.isAllMatched()) {
                String reason = createOnBeanNoMatchReason(matchResult);
                return ConditionOutcome.noMatch(spec.message().because(reason));
            }
            matchMessage = spec.message(matchMessage).found("bean", "beans").items(Style.QUOTE,
                    matchResult.getNamesOfAllMatches());
        }
        // ConditionalOnSingleCandidate 注解处理
        // ConditionalOnMissingBean 注解处理
        if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
            Spec<ConditionalOnMissingBean> spec = new Spec<>(context, metadata, annotations,
                    ConditionalOnMissingBean.class);
            MatchResult matchResult = getMatchingBeans(context, spec);
            if (matchResult.isAnyMatched()) {
                String reason = createOnMissingBeanNoMatchReason(matchResult);
                return ConditionOutcome.noMatch(spec.message().because(reason));
            }
            matchMessage = spec.message(matchMessage).didNotFind("any beans").atAll();
        }
        return ConditionOutcome.match(matchMessage);
    }
}
```

从代码的层面上，我们可以很清楚的看出，`@ConditionalOnMissingBean`的处理方式和`@ConditionalOnBean`是完全相反的。

不过，我们之前说的`@ConditionalOnMissingBean`多了一个处理被忽略类型的 `bean`，这个一般是处理父类的。

```java
public @interface ConditionalOnMissingBean {
    
    Class<?>[] ignored() default {};

	String[] ignoredType() default {};
}
```

我们跑一个例子看看

```java
@Configuration
public class BeanConfig {

    @Bean(name = "desktopPC")
    public DesktopPC desktopPC(){
        return new DesktopPC("台式电脑", "华硕全家桶");
    }

    @ConditionalOnMissingBean(value = Computer.class)
    @Bean("notebookPC")
    public Computer computer(){
        return new Computer("笔记本电脑");
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
```

执行结果：

![image-20240122180745432](.\conditional.assets\image-20240122180745432.png)

稍微修改一下

```java
@Configuration
public class BeanConfig {

    @Bean(name = "desktopPC")
    public DesktopPC desktopPC(){
        return new DesktopPC("台式电脑", "华硕全家桶");
    }

    @ConditionalOnMissingBean(value = Computer.class, ignored = DesktopPC.class)
    @Bean("notebookPC")
    public Computer computer(){
        return new Computer("笔记本电脑");
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
```

执行结果：

![image-20240122180717959](.\conditional.assets\image-20240122180717959.png)

我们进入到源码看看

![image-20240122184943377](.\conditional.assets\image-20240122184943377.png)

可以看到，多了一个ignoredTypes。

```java
this.ignoredTypes = extract(attributes, "ignored", "ignoredType");
```

继续往下看

```java
class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {
    private Set<String> getNamesOfBeansIgnoredByType(ClassLoader classLoader, ListableBeanFactory beanFactory,boolean considerHierarchy, Set<String> ignoredTypes, Set<Class<?>> parameterizedContainers) {
        Set<String> result = null;
        for (String ignoredType : ignoredTypes) {
            Collection<String> ignoredNames = getBeanNamesForType(classLoader, considerHierarchy, beanFactory, ignoredType, parameterizedContainers);
            result = addAll(result, ignoredNames);
        }
        return (result != null) ? result : Collections.emptySet();
    }

    private Set<String> getBeanNamesForType(ClassLoader classLoader, boolean considerHierarchy, ListableBeanFactory beanFactory, String type, Set<Class<?>> parameterizedContainers) throws LinkageError {
        try {
            return getBeanNamesForType(beanFactory, considerHierarchy, resolve(type, classLoader),
                                       parameterizedContainers);
        }
        catch (ClassNotFoundException | NoClassDefFoundError ex) {
            return Collections.emptySet();
        }
    }

    private Set<String> getBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type, Set<Class<?>> parameterizedContainers) {
        Set<String> result = collectBeanNamesForType(beanFactory, considerHierarchy, type, parameterizedContainers,
                                                     null);
        return (result != null) ? result : Collections.emptySet();
    }

    private Set<String> collectBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type, Set<Class<?>> parameterizedContainers, Set<String> result) {
        result = addAll(result, beanFactory.getBeanNamesForType(type, true, false));
        for (Class<?> container : parameterizedContainers) {
            ResolvableType generic = ResolvableType.forClassWithGenerics(container, type);
            result = addAll(result, beanFactory.getBeanNamesForType(generic, true, false));
        }
        if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory) {
            // 做递归查看父工厂
            BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
            if (parent instanceof ListableBeanFactory) {
                result = collectBeanNamesForType((ListableBeanFactory) parent, considerHierarchy, type, parameterizedContainers, result);
            }
        }
        return result;
    }
}
```

这就生成了`beansIgnoredByType`，然后在后面的匹配中进行排除。



### 2.6.3、彩蛋

有时候我们会看到某一个配置上只写了一个@ConditionalOnMissingBean，类似于

![image-20240204103858400](.\conditional.assets\image-20240204103858400.png)

这种情况一般是指，返回值的这个类要求是单例，也就是说，如果有别的地方已经完成了实例化，这里就不会再实例化了

这里我们可以到Spec的构造方法中看到

```java
private static class Spec<A extends Annotation> {
    Spec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations,
         Class<A> annotationType) {
        MultiValueMap<String, Object> attributes = annotations.stream(annotationType)
            .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
            .collect(MergedAnnotationCollectors.toMultiValueMap(Adapt.CLASS_TO_STRING));
        MergedAnnotation<A> annotation = annotations.get(annotationType);
        this.classLoader = context.getClassLoader();
        this.annotationType = annotationType;
        this.names = extract(attributes, "name");
        this.annotations = extract(attributes, "annotation");
        this.ignoredTypes = extract(attributes, "ignored", "ignoredType");
        this.parameterizedContainers = resolveWhenPossible(extract(attributes, "parameterizedContainer"));
        this.strategy = annotation.getValue("search", SearchStrategy.class).orElse(null);
        Set<String> types = extractTypes(attributes);
        BeanTypeDeductionException deductionException = null;
        if (types.isEmpty() && this.names.isEmpty()) {
            try {
                types = deducedBeanType(context, metadata);
            }
            catch (BeanTypeDeductionException ex) {
                deductionException = ex;
            }
        }
        this.types = types;
        validate(deductionException);
    }

    private Set<String> deducedBeanType(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (metadata instanceof MethodMetadata && metadata.isAnnotated(Bean.class.getName())) {
            return deducedBeanTypeForBeanMethod(context, (MethodMetadata) metadata);
        }
        return Collections.emptySet();
    }

    private Set<String> deducedBeanTypeForBeanMethod(ConditionContext context, MethodMetadata metadata) {
        try {
            Class<?> returnType = getReturnType(context, metadata);
            return Collections.singleton(returnType.getName());
        }
        catch (Throwable ex) {
            throw new BeanTypeDeductionException(metadata.getDeclaringClassName(), metadata.getMethodName(), ex);
        }
    }
}
```

不过这里而我们在日常使用中，可能有一个小问题：

- 注意返回值需不需要设置成接口，也就是说，如果设置成接口，就是让 接口 只有一个实现的bean。



## 2.6、@ConditionalOnSingleCandidate

这个注解，平时接触的可能比较少，在这里做一个顺带介绍吧

@ConditionalOnSingleCandidate对应的Condition处理类也是OnBeanCondition。

如果当指定Bean在容器中只有一个，或者虽然有多个但是指定首选Bean的时候则生效。

```java
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnSingleCandidate {

    /**
     * 需要作为条件的类的Class对象
     */
	Class<?> value() default Object.class;

    /**
     * 需要作为条件的类的Name, Class.getName()
     */
	String type() default "";

    /**
     * 搜索容器层级，当前容器，父容器
     */
	SearchStrategy search() default SearchStrategy.ALL;

}
```

直接到使用环节！

### 2.6.1、怎么用

```java
@Configuration
public class BeanSingleCandidateConfig {

    @Bean
    public Computer desktopPC() {
        return new Computer("台式电脑");
    }

    @Bean
    public Computer computer() {
        return new Computer("笔记本电脑");
    }

    /**
     * 一个学生只能有一个电脑
     *
     * @return -
     */
    @Bean(name = "小明")
    @ConditionalOnSingleCandidate(Computer.class)
    public Student getStudent() {
        return new Student("小明");
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanSingleCandidateConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
```

运行结果：

![image-20240202182307374](.\conditional.assets\image-20240202182307374.png)

可以看到，`student.class`没有被初始化

我们注释掉其中一个

```java
@Configuration
public class BeanSingleCandidateConfig {

    @Bean
    public Computer desktopPC() {
        return new Computer("台式电脑");
    }

/*    @Bean
    public Computer computer() {
        return new Computer("笔记本电脑");
    }*/

    /**
     * 一个学生只能有一个电脑
     *
     * @return -
     */
    @Bean(name = "小明")
    @ConditionalOnSingleCandidate(Computer.class)
    public Student getStudent() {
        return new Student("小明");
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanSingleCandidateConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
```

运行结果：

![image-20240202182512646](.\conditional.assets\image-20240202182512646.png)

结果上很清楚的看到结果！



### 2.6.2、源码分析

还是回到我们熟悉的那个方法

```java
class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConditionMessage matchMessage = ConditionMessage.empty();
        MergedAnnotations annotations = metadata.getAnnotations();
        // ConditionalOnBean 注解处理
        // ConditionalOnSingleCandidate 注解处理
        if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
            Spec<ConditionalOnSingleCandidate> spec = new SingleCandidateSpec(context, metadata, annotations);
            MatchResult matchResult = getMatchingBeans(context, spec);
            if (!matchResult.isAllMatched()) {
                return ConditionOutcome.noMatch(spec.message().didNotFind("any beans").atAll());
            }
            Set<String> allBeans = matchResult.getNamesOfAllMatches();
            if (allBeans.size() == 1) {
                matchMessage = spec.message(matchMessage).found("a single bean").items(Style.QUOTE, allBeans);
            } else {
                List<String> primaryBeans = getPrimaryBeans(context.getBeanFactory(), allBeans,
                        spec.getStrategy() == SearchStrategy.ALL);
                if (primaryBeans.isEmpty()) {
                    return ConditionOutcome.noMatch(
                            spec.message().didNotFind("a primary bean from beans").items(Style.QUOTE, allBeans));
                }
                if (primaryBeans.size() > 1) {
                    return ConditionOutcome
                            .noMatch(spec.message().found("multiple primary beans").items(Style.QUOTE, primaryBeans));
                }
                matchMessage = spec.message(matchMessage)
                        .found("a single primary bean '" + primaryBeans.get(0) + "' from beans")
                        .items(Style.QUOTE, allBeans);
            }
        }
        // ConditionalOnMissingBean 注解处理
    }
    
    protected final MatchResult getMatchingBeans(ConditionContext context, Spec<?> spec) {
		ClassLoader classLoader = context.getClassLoader();
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		boolean considerHierarchy = spec.getStrategy() != SearchStrategy.CURRENT;
		Set<Class<?>> parameterizedContainers = spec.getParameterizedContainers();
        //跳过
		if (spec.getStrategy() == SearchStrategy.ANCESTORS) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			Assert.isInstanceOf(ConfigurableListableBeanFactory.class, parent,
					"Unable to use SearchStrategy.ANCESTORS");
			beanFactory = (ConfigurableListableBeanFactory) parent;
		}
		MatchResult result = new MatchResult();
        //这块是@ConditionalOnMissingBean 的，不管
		Set<String> beansIgnoredByType = getNamesOfBeansIgnoredByType(classLoader, beanFactory, considerHierarchy,
				spec.getIgnoredTypes(), parameterizedContainers);
        
        // 核心，获取类的实例
		for (String type : spec.getTypes()) {
			Collection<String> typeMatches = getBeanNamesForType(classLoader, considerHierarchy, beanFactory, type,
					parameterizedContainers);
			Iterator<String> iterator = typeMatches.iterator();
			while (iterator.hasNext()) {
				String match = iterator.next();
                  //这块是@ConditionalOnMissingBean 的，不管
				if (beansIgnoredByType.contains(match) || ScopedProxyUtils.isScopedTarget(match)) {
					iterator.remove();
				}
			}
			if (typeMatches.isEmpty()) {
				result.recordUnmatchedType(type);
			}
			else {
				result.recordMatchedType(type, typeMatches);
			}
		}
		// ....
		return result;
	}
    
	private Set<String> getBeanNamesForType(ClassLoader classLoader, boolean considerHierarchy,
			ListableBeanFactory beanFactory, String type, Set<Class<?>> parameterizedContainers) throws LinkageError {
		try {
			return getBeanNamesForType(beanFactory, considerHierarchy, resolve(type, classLoader),
					parameterizedContainers);
		}
		catch (ClassNotFoundException | NoClassDefFoundError ex) {
			return Collections.emptySet();
		}
	}
    
    
	private Set<String> getBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type,
			Set<Class<?>> parameterizedContainers) {
		Set<String> result = collectBeanNamesForType(beanFactory, considerHierarchy, type, parameterizedContainers,
				null);
		return (result != null) ? result : Collections.emptySet();
	}

	private Set<String> collectBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy,
			Class<?> type, Set<Class<?>> parameterizedContainers, Set<String> result) {
		result = addAll(result, beanFactory.getBeanNamesForType(type, true, false));
		for (Class<?> container : parameterizedContainers) {
			ResolvableType generic = ResolvableType.forClassWithGenerics(container, type);
			result = addAll(result, beanFactory.getBeanNamesForType(generic, true, false));
		}
		if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory) {
			BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
			if (parent instanceof ListableBeanFactory) {
				result = collectBeanNamesForType((ListableBeanFactory) parent, considerHierarchy, type,
						parameterizedContainers, result);
			}
		}
		return result;
	}
}
```

这些代码前面我们都看过，就不详细介绍了

![image-20240204100723154](.\conditional.assets\image-20240204100723154.png)

![image-20240204102122824](.\conditional.assets\image-20240204102122824.png)



## **2.7、@ConditionalOnClass**

这个注解，我们从名字上就能够很清楚的看出，注入的条件是Class。也就是说，当给定的类名在类路径上存在，则实例化当前Bean。这也是一个比较重要的注解，我们看看代码

```java
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnClassCondition.class)
public @interface ConditionalOnClass {

	Class<?>[] value() default {};

	String[] name() default {};

}
```

可以看到，这个和之前的`@ConditionalOnMissingBean`和`@ConditionalOnBean`不一样了。涉及的是`OnClassCondition`类

而且，我们从注释中可以看到，`@ConditionalOnClass`没有和`@ConditionalOnBean`一样强调顺序性。

### 2.7.1、怎么用

这个的使用和`@ConditionalOnBean`差不多，而且Spring中有大量的例子，就不做举例了

我们直接进入源码阶段。



### 2.7.2、源码分析

`OnClassCondition`类其实和`OnBeanCondition`差不多，都是实现了`FilteringSpringBootCondition`类，所以最主要的，还是`getMatchOutcome`方法

```java
class OnClassCondition extends FilteringSpringBootCondition {

    // 该方法分三部分
    // 1. 处理ConditionalOnClass注解
    // 2. 处理ConditionalOnMissingClass注解
    // 3. 返回条件判断的结果
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ClassLoader classLoader = context.getClassLoader();
        ConditionMessage matchMessage = ConditionMessage.empty();

        // 1. 处理ConditionalOnClass注解
        //获得@ConditionalOnClass注解中配置的value和name属性的值
        // 如有加载不到的则放到missing中
        List<String> onClasses = getCandidates(metadata, ConditionalOnClass.class);
        if (onClasses != null) {
            // filter()方法内部 对onClass表示的类进行反射，条件为MISSING，
            // 如果得到的集合不为空，则说明类路径中不存在ConditionalOnClass注解中标注的类
            // 这种情况下直接通过ConditionOutcome.noMatch()封装ConditionOutcome条件判断的结果并返回，noMatch()即表示不通过。
            List<String> missing = filter(onClasses, ClassNameFilter.MISSING, classLoader);
            if (!missing.isEmpty()) {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
                                                .didNotFind("required class", "required classes").items(Style.QUOTE, missing));
            }

            // 对ConditionalOnClass注解的条件判断通过，并保存对应的信息到matchMessage
            matchMessage = matchMessage.andCondition(ConditionalOnClass.class)
                .found("required class", "required classes")
                .items(Style.QUOTE, filter(onClasses, ClassNameFilter.PRESENT, classLoader));
        }

        // 2. 处理ConditionalOnMissingClass注解
        // 获取该元数据表示的类或方法上的ConditionalOnMissingClass注解中标注的类的限定名，
        // 表示这些类应当在classpath类路径中不存在，所以叫onMissingClass
        // 例如：@ConditionalOnMissingClass({ RabbitTemplate.class, Channel.class })，
        //      则返回RabbitTemplate和Channel的全限定类名
        List<String> onMissingClasses = getCandidates(metadata, ConditionalOnMissingClass.class);
        if (onMissingClasses != null) {
            // filter()方法内部 对onMissingClasses表示的类进行反射，条件为PRESENT，
            // 如果得到的集合不为空，则说明类路径中存在ConditionalOnMissingClass注解中标注的类
            // 这种情况下直接通过ConditionOutcome.noMatch()封装ConditionOutcome条件判断的结果并返回，noMatch()即表示不通过。
            List<String> present = filter(onMissingClasses, ClassNameFilter.PRESENT, classLoader);
            if (!present.isEmpty()) {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnMissingClass.class)
                                                .found("unwanted class", "unwanted classes").items(Style.QUOTE, present));
            }

            // 对ConditionalOnMissingClass注解的条件判断通过，并保存对应的信息到matchMessage
            matchMessage = matchMessage.andCondition(ConditionalOnMissingClass.class)
                .didNotFind("unwanted class", "unwanted classes")
                .items(Style.QUOTE, filter(onMissingClasses, ClassNameFilter.MISSING, classLoader));
        }

        // 3. 返回条件判断的结果，到这一步，就说明ConditionalOnClass注解和ConditionalOnMissingClass注解上的条件都已经通过了。
        return ConditionOutcome.match(matchMessage);
    }

    private List<String> getCandidates(AnnotatedTypeMetadata metadata, Class<?> annotationType) {
        MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(annotationType.getName(), true);
        if (attributes == null) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        addAll(candidates, attributes.get("value"));
        addAll(candidates, attributes.get("name"));
        return candidates;
    }

    /**
     * 判断是否加载到
     */
    protected final List<String> filter(Collection<String> classNames, ClassNameFilter classNameFilter,
                                        ClassLoader classLoader) {
        if (CollectionUtils.isEmpty(classNames)) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<>(classNames.size());
        for (String candidate : classNames) {
            if (classNameFilter.matches(candidate, classLoader)) {
                matches.add(candidate);
            }
        }
        return matches;
    }
}

```

![image-20240204154811391](.\conditional.assets\image-20240204154811391.png)

![image-20240204154929096](.\conditional.assets\image-20240204154929096.png)

可以看到，**@ConditionalOnClass注解中判断配置的类是否存在使用的方法是Class.forName，类加载。**

对于@ConditionalOnMissingClass，也是一样的逻辑，这里就不展开说明了

