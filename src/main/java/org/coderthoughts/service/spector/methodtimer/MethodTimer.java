package org.coderthoughts.service.spector.methodtimer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.coderthoughts.service.spector.ServiceAspect;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(scope=ServiceScope.PROTOTYPE)
public class MethodTimer implements ServiceAspect {
    Map<String, Queue<Long>> invocationDurations = new ConcurrentHashMap<>();
    ThreadLocal<Long> startNanoThreadLocal = new ThreadLocal<>();

    @Override
    public String announce() {
        return "Invocation Duration measurement started.\n";
    }

    @Override
    public void preServiceInvoke(ServiceReference<?> service, Method method, Object[] args) {
        startNanoThreadLocal.set(System.nanoTime());
    }

    @Override
    public void postServiceInvoke(ServiceReference<?> service, Method method, Object[] args, Object result) {
        long endNanos = System.nanoTime();
        long startNanos = startNanoThreadLocal.get();
        long duration = endNanos - startNanos;
        startNanoThreadLocal.remove();

        Class<?> declaringClass = method.getDeclaringClass();
        String key = declaringClass.getSimpleName() + "#" + method.getName();
        invocationDurations.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(duration);
    }

    @Override
    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("Invocation duration (nanoseconds)\n");
        sb.append("=================================\n");
        for (Map.Entry<String, Queue<Long>> entry : invocationDurations.entrySet()) {
            long average = (long) entry.getValue().stream().mapToLong(Long::longValue).average().getAsDouble();
            sb.append(entry.getKey() + ": " + average + "\n");
        }
        return sb.toString();
    }
}
