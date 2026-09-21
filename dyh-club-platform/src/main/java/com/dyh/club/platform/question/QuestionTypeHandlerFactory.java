package com.dyh.club.platform.question;

import com.dyh.club.platform.common.BizException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class QuestionTypeHandlerFactory {
    private final Map<String, QuestionTypeHandler> handlers;

    public QuestionTypeHandlerFactory(List<QuestionTypeHandler> values) {
        Map<String, QuestionTypeHandler> mapped = new HashMap<>();
        for (QuestionTypeHandler handler : values) {
            if (mapped.put(handler.type(), handler) != null) throw new IllegalStateException("重复题型策略: " + handler.type());
        }
        for (String required : new String[]{"RADIO", "MULTIPLE", "JUDGE", "BRIEF"}) {
            if (!mapped.containsKey(required)) throw new IllegalStateException("缺少题型策略: " + required);
        }
        handlers = Collections.unmodifiableMap(mapped);
    }

    public QuestionTypeHandler get(String type) {
        QuestionTypeHandler handler = handlers.get(type == null ? "" : type.toUpperCase(Locale.ROOT));
        if (handler == null) throw BizException.badRequest("不支持的题型: " + type);
        return handler;
    }
}
