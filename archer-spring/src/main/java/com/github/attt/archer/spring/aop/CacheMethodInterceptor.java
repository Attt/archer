package com.github.attt.archer.spring.aop;

import com.github.attt.archer.exception.CacheOperationException;
import com.github.attt.archer.interceptor.InvocationInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;

import java.lang.reflect.Method;

/**
 * Cache method interceptor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheMethodInterceptor implements MethodInterceptor {

    public static final String CACHE_METHOD_INTERCEPTOR_BEAN_NAME = "archer.internalMethodInterceptor";

    private final static Logger logger = LoggerFactory.getLogger(CacheMethodInterceptor.class);

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object[] args = methodInvocation.getArguments();
        Method method = methodInvocation.getMethod();
        Object target = methodInvocation.getThis();
        Object proxy = null;
        if (methodInvocation instanceof ReflectiveMethodInvocation) {
            proxy = ((ReflectiveMethodInvocation) methodInvocation).getProxy();
        }

        return InvocationInterceptor.INSTANCE.invoke(proxy, target, () -> {
            try {
                return methodInvocation.proceed();
            } catch (Throwable throwable) {
                throw new CacheOperationException(throwable);
            }
        }, method, args);
    }

}
