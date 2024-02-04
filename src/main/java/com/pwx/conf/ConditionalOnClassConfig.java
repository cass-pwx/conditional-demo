package com.pwx.conf;

import com.pwx.model.School;
import com.pwx.model.Student;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author pengweixin
 */

@Configuration
public class ConditionalOnClassConfig {

    @Bean
    public School school() {
        return new School("清华大学");
    }

    @ConditionalOnClass(School.class)
    @Bean("zhangsan")
    public Student student() {
        return new Student("张三");
    }



    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ConditionalOnClassConfig.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        // 遍历Spring容器中的beanName
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }
    }
}
