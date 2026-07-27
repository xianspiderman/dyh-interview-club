package com.dyh.club.subject.domain.util;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.dyh.club.subject.domain.entity.SubjectCategoryBO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存工具类
 *
 */

@Component
public class CacheUtil<K, V> {

    private Cache<String, String> localCache =
            CacheBuilder.newBuilder()
                    .maximumSize(5000) //上限5000
                    .expireAfterWrite(10, TimeUnit.SECONDS) //存活10s
                    .build();


    public List<V> getResult(String cacheKey, Class<V> clazz,
                             Function<String, List<V>> function) {
        // Class<V>要传一个对象过来，要不序列化json不知道长啥样子；  Function可以传一个函数进来
        List<V> resultList = new ArrayList<>();
        String content = localCache.getIfPresent(cacheKey);
        if (StringUtils.isNotBlank(content)) {// 看看有没有缓存，不为空就返回已有的缓存
            resultList = JSON.parseArray(content, clazz); // clazz是一个对象，让他知道长啥样子
        } else {
            resultList = function.apply(cacheKey); //缓存为空，apply就是运行Function函数，得出数据
            if (!CollectionUtils.isEmpty(resultList)) {
                localCache.put(cacheKey, JSON.toJSONString(resultList)); // 把运行后的数据装进缓存
            }
        }
        return resultList;
    }

    public Map<K, V> getMapResult(String cacheKey, Class<V> clazz,
                                  Function<String, Map<K, V>> function) {
        return new HashMap<>(); // 这个一般不用，作者没写
    }

}
