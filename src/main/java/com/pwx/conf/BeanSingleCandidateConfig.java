package com.pwx.conf;

import com.pwx.model.Computer;
import com.pwx.model.Student;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author pengweixin
 */

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
