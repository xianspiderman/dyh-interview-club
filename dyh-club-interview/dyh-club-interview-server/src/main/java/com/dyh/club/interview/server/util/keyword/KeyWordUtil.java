package com.dyh.club.interview.server.util.keyword;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;

import java.util.*;

public class KeyWordUtil {

    /**
     * 敏感词字典
     */
    private final static Map wordMap = new HashMap(1024);
    private static boolean init = false;

    public static boolean isInit() {
        return init;
    }

    /**
     * 获取敏感词列表
     *
     * @param text 输入文本
     */
    public static List<String> buildKeyWordsLists(final String text) {

        List<String> wordList = new ArrayList<>();
        char[] charset = text.toCharArray();
        for (int i = 0; i < charset.length; i++) {
            FlagIndex fi = getFlagIndex(charset, i, 0);
            if (fi.isFlag()) {
                if (fi.isWhiteWord()) {
                    i += fi.getIndex().size() - 1;
                } else {
                    StringBuilder builder = new StringBuilder();
                    for (int j : fi.getIndex()) {
                        char word = text.charAt(j);
                        builder.append(word);
                    }
                    wordList.add(builder.toString());
                }
            }
        }
        return wordList;

    }


    /**
     * 获取标记索引
     *
     * @param charset 输入文本
     * @param begin   检测起始
     * @param skip    文本距离
     */
    private static FlagIndex getFlagIndex(final char[] charset, final int begin, final int skip) {

        FlagIndex fi = new FlagIndex();
        Map current = wordMap;
        boolean flag = false;
        int count = 0;
        List<Integer> index = new ArrayList<>();
        for (int i = begin; i < charset.length; i++) {
            char word = charset[i];
            Map mapTree = (Map) current.get(word);
            if (count > skip || (i == begin && Objects.isNull(mapTree))) {
                break;
            }
            if (Objects.nonNull(mapTree)) {
                current = mapTree;
                count = 0;
                index.add(i);
            } else {
                count++;
                if (flag && count > skip) {
                    break;
                }
            }
            if ("1".equals(current.get("isEnd"))) {
                flag = true;
            }
            if ("1".equals(current.get("isWhiteWord"))) {
                fi.setWhiteWord(true);
                break;
            }
        }
        fi.setFlag(flag);
        fi.setIndex(index);
        return fi;

    }

    public static void addWord(Collection<String> wordList) {
        init = true;
        if (CollectionUtils.isEmpty(wordList)) {
            return;
        }
        WordType wordType = WordType.BLACK;
        Map nowMap;
        Map<String, String> newWorMap;
        // 迭代keyWordSet
        for (String key : wordList) {
            nowMap = wordMap;
            for (int i = 0; i < key.length(); i++) {
                // 转换成char型
                char keyChar = key.charAt(i);
                // 获取
                Object wordMap = nowMap.get(keyChar);
                // 如果存在该key，直接赋值
                if (wordMap != null) {
                    nowMap = (Map) wordMap;
                } else {
                    // 不存在则构建一个map，同时将isEnd设置为0，因为他不是最后一个
                    newWorMap = new HashMap<>(4);
                    // 不是最后一个
                    newWorMap.put("isEnd", String.valueOf(EndType.HAS_NEXT.ordinal()));
                    nowMap.put(keyChar, newWorMap);
                    nowMap = newWorMap;
                }

                if (i == key.length() - 1) {
                    // 最后一个
                    nowMap.put("isEnd", String.valueOf(EndType.IS_END.ordinal()));
                    nowMap.put("isWhiteWord", String.valueOf(wordType.ordinal()));
                }
            }
        }
    }
}
