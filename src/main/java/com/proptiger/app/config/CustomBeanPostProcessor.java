package com.proptiger.app.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.proptiger.core.init.CustomObjectMapper;

public class CustomBeanPostProcessor implements BeanPostProcessor{

	@Override
	public Object postProcessAfterInitialization(Object bean, String arg1) throws BeansException {
		// TODO Auto-generated method stub
		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String arg1) throws BeansException {
		if (bean != null && bean instanceof CustomObjectMapper) {
            CustomObjectMapper converter = (CustomObjectMapper) bean;
            SimpleModule module = new SimpleModule("customerSerializationModule");

            converter.registerModule(module);
        }
		// TODO Auto-generated method stub
		return bean;
	}
	

}
