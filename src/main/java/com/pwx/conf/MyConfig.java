package com.pwx.conf;

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