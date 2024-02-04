package com.pwx.conf;

import com.pwx.model.Computer;
import com.pwx.model.DesktopPC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author pengweixin
 */

@Configuration
public class BeanConfig {

    @Bean(name = "desktopPC")
    public DesktopPC desktopPC(){
        return new DesktopPC("台式电脑", "华硕全家桶");
    }

    @ConditionalOnMissingBean
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
