package com.dyh.club.platform;

import com.dyh.club.platform.common.UserContext;
import com.dyh.club.platform.config.AsyncConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Future;
import static org.assertj.core.api.Assertions.assertThat;

class AsyncContextTest {
    @Test void userAndTraceContextAreTransferredAndCleaned() throws Exception {
        ThreadPoolTaskExecutor executor=new AsyncConfig().labelExecutor(1,1,10);
        try {
            UserContext.set(42L);MDC.put("traceId","trace-test");
            Future<String> first=executor.submit(()->UserContext.get()+":"+MDC.get("traceId"));
            assertThat(first.get()).isEqualTo("42:trace-test");
            UserContext.clear();MDC.clear();
            Future<String> second=executor.submit(()->String.valueOf(UserContext.get())+":"+MDC.get("traceId"));
            assertThat(second.get()).isEqualTo("null:null");
        } finally {UserContext.clear();MDC.clear();executor.shutdown();}
    }
}
