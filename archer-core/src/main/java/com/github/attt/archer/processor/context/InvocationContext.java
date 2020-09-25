package com.github.attt.archer.processor.context;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Invocation context
 *
 * @author atpexgo
 * @since 1.0
 */
public class InvocationContext {

    private transient Object target;

    private transient Method method;

    private transient Object[] args;

    private transient Supplier<?> methodInvoker;

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Supplier<?> getMethodInvoker() {
        return methodInvoker;
    }

    public void setMethodInvoker(Supplier<?> methodInvoker) {
        this.methodInvoker = methodInvoker;
    }


    public InvocationContext() {

    }

    public InvocationContext(Object target, Method method, Object[] args, Supplier<?> methodInvoker) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.methodInvoker = methodInvoker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvocationContext)) return false;
        InvocationContext that = (InvocationContext) o;
        return Objects.equals(target, that.target) &&
                Objects.equals(method, that.method) &&
                Arrays.equals(args, that.args) &&
                Objects.equals(methodInvoker, that.methodInvoker);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(target, method, methodInvoker);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }

    @Override
    public String toString() {
        return "InvocationContext{" +
                "target=" + target +
                ", method=" + method +
                ", args=" + Arrays.toString(args) +
                ", methodInvoker=" + methodInvoker +
                '}';
    }
}
