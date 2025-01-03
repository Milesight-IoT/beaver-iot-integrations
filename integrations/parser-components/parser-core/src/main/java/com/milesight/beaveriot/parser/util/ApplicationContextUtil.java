package com.milesight.beaveriot.parser.util;

import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author linzy
 */
@Component
public class ApplicationContextUtil implements ApplicationContextAware {

    @Setter
    private static ApplicationContext context;
    @Setter
    private static DefaultListableBeanFactory defaultListableBeanFactory;

    private ApplicationContextUtil() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextUtil.setContext(applicationContext);
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        ApplicationContextUtil.setDefaultListableBeanFactory((DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory());
    }

    public static void registerBean(String beanName, Class<?> clazz) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());
    }

    public static void removeBean(String beanName) {
        defaultListableBeanFactory.removeBeanDefinition(beanName);
    }

    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

}