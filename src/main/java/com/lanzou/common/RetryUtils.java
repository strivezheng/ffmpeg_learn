package com.lanzou.common;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

/**
 * <p>这是一套轻量级的重试框架</p>
 *
 * @date 2020年03月27日 10:07
 */
@Slf4j
public class RetryUtils {

    private static RetryEngine DEFAULT_ENGINE = new RetryEngine();

    /**
     * 默认的重试执行，3次，间隔1000ms
     * @param task  有返回值任务
     * @return 返回值
     */
    public static <T> T run(RetryTask<T> task) {
        return DEFAULT_ENGINE.run(task);
    }

    /**
     * 默认的重试执行，3次，间隔1000ms
     * @param task  无返回值任务
     */
    public static void run(VoidRetryTask task) {
        DEFAULT_ENGINE.run(task);
    }

    /**
     * 重试执行
     *
     * @param task          要重试的任务
     * @param maxAttempts   最大重试次数
     * @param period        每次重试间隔
     * @param <T>           返回值
     * @return              result
     */
    public static <T> T run(int maxAttempts, long period, RetryTask<T> task) {
        RetryEngine engine = generateRetryEngine(maxAttempts, period);
        return engine.run(task);
    }


    /**
     * 重试执行
     *
     * @param task          要重试的任务
     * @param maxAttempts   最大重试次数
     * @param period        每次重试间隔
     */
    public static void run(int maxAttempts, long period, VoidRetryTask task) {
        RetryEngine engine = generateRetryEngine(maxAttempts, period);
        engine.run(task);
    }


    /**
     * 构造一个重试器
     *
     * @param maxAttempts   最大重试次数
     * @param period        每次重试间隔
     * @return 重试器
     */
    public static RetryEngine generateRetryEngine(int maxAttempts, long period) {
        return new RetryEngine(maxAttempts, period);
    }

    @Data
    public static class RetryEngine {
        private int maxAttempts = 3;
        private long period = 1000L;
        private AfterThrowTask afterThrowTask;
        private FinalTask finalTask;

        public RetryEngine() {
        }

        public RetryEngine(int maxAttempts, long period) {
            this.maxAttempts = maxAttempts;
            this.period = period;
        }

        public <T> T run(RetryTask<T> task) {
            RetryContext context = new RetryContext(0, null, this);

            for (int i = 1; i <= maxAttempts; i++) {
                try {
                    return task.execute();
                } catch (Exception e) {
                    context.failCount = i;
                    context.exception = e;

                    log.info("- fail count = {}", context.failCount);
                    log.info("- error message:", context.exception);

                    if (afterThrowTask != null) {
                        afterThrowTask.execute(context);
                    }
                    if (i < maxAttempts) {
                        LockSupport.parkNanos(period * 1000000L);
                    }
                }
            }

            log.info("- retry failed after {} times!", maxAttempts);

            if (finalTask != null) {
                return finalTask.execute(context);
            }

            throw new RuntimeException(context.exception);
        }

        public void run(VoidRetryTask task) {
            RetryContext context = new RetryContext(0, null, this);

            for (int i = 1; i <= maxAttempts; i++) {
                try {
                    task.execute();
                    return;
                } catch (Exception e) {
                    context.failCount++;
                    context.exception = e;

                    log.info("- execute count = {}, fail count = {}", i, context.failCount);
                    log.info("- error message:", context.exception);

                    if (afterThrowTask != null) {
                        afterThrowTask.execute(context);
                    }
                    if (i < maxAttempts) {
                        LockSupport.parkNanos(period * 1000000L);
                    }
                }
            }

            log.info("- retry failed after {} times!", maxAttempts);

            if (finalTask != null) {
                finalTask.execute(context);
                return;
            }

            throw new RuntimeException(context.exception);
        }
    }



    public interface RetryTask<T> {
        T execute();
    }

    public interface VoidRetryTask {
        void execute();
    }

    public interface AfterThrowTask {
        void execute(RetryContext context);
    }

    public interface FinalTask {
        <T> T execute(RetryContext context);
    }

    public static class RetryContext {
        int failCount;
        Exception exception;
        RetryEngine template;

        public RetryContext(int failCount, Exception exception, RetryEngine template) {
            this.failCount = failCount;
            this.exception = exception;
            this.template = template;
        }
    }


}