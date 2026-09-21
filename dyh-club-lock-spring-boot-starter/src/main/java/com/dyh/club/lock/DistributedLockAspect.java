package com.dyh.club.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
public class DistributedLockAspect {
    private final LockExecutor executor;
    private final LockProperties properties;
    private final ExpressionParser parser = new SpelExpressionParser();

    public DistributedLockAspect(LockExecutor executor, LockProperties properties) {
        this.executor = executor;
        this.properties = properties;
    }

    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint point, DistributedLock lock) throws Throwable {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        String[] parameterNames = ((MethodSignature) point.getSignature()).getParameterNames();
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = point.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (parameterNames != null && i < parameterNames.length) context.setVariable(parameterNames[i], args[i]);
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
        }
        Object resource = parser.parseExpression(lock.key()).getValue(context);
        if (resource == null) {
            throw new IllegalArgumentException("分布式锁资源标识不能为空: " + method.getName());
        }
        String key = properties.getKeyPrefix() + lock.prefix() + ":" + resource;
        String owner = UUID.randomUUID().toString();
        long wait = lock.waitMillis() < 0 ? properties.getWaitMillis() : lock.waitMillis();
        long lease = lock.leaseMillis() < 0 ? properties.getLeaseMillis() : lock.leaseMillis();
        if (!executor.tryLock(key, owner, wait, lease)) {
            throw new LockBusyException("资源正在处理中，请稍后重试");
        }
        try {
            return point.proceed();
        } finally {
            executor.unlock(key, owner);
        }
    }
}
