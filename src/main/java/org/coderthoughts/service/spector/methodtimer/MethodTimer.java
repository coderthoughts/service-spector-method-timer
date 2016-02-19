package org.coderthoughts.service.spector.methodtimer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.coderthoughts.service.spector.ServiceAspect;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(scope=ServiceScope.PROTOTYPE)
public class MethodTimer implements ServiceAspect {
    Map<String, Queue<Long>> invocationDurations = new ConcurrentHashMap<>();
    ThreadLocal<Long> startNanoThreadLocal = new ThreadLocal<>();

    @Override
    public void preServiceInvoke(Object service, Method method, Object[] args) throws Exception {
        startNanoThreadLocal.set(System.nanoTime());
    }

    @Override
    public void postServiceInvoke(Object service, Method method, Object[] args, Object result) throws Exception {
        long endNanos = System.nanoTime();
        long startNanos = startNanoThreadLocal.get();
        long duration = endNanos - startNanos;
        startNanoThreadLocal.remove();

        Class<?> declaringClass = method.getDeclaringClass();
        String key = declaringClass.getSimpleName() + "#" + method.getName();
        invocationDurations.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(duration);
    }

    @Override
    public void report() {
        System.out.println("Invocation duration (nanoseconds)");
        System.out.println("=================================");
        for (Map.Entry<String, Queue<Long>> entry : invocationDurations.entrySet()) {
            long average = (long) entry.getValue().stream().mapToLong(Long::longValue).average().getAsDouble();
            System.out.println(entry.getKey() + ": " + average);
        }
        System.out.println();
    }
}
