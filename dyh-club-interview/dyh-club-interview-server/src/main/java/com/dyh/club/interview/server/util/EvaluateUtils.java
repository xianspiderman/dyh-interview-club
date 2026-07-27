package com.dyh.club.interview.server.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EvaluateUtils {

    private static final List<Evaluate> evaluates = build();

    private static List<Evaluate> build() {
        List<Evaluate> list = new ArrayList<>();
        list.add(new Evaluate(0, 3, "%s掌握较差,要加把劲"));
        list.add(new Evaluate(0, 3, "%s掌握不大行,还要努努力"));
        list.add(new Evaluate(0, 3, "%s掌握有点差,要加把劲"));
        list.add(new Evaluate(0, 3, "%s掌握还是比较弱,多跟鸡哥学学"));
        list.add(new Evaluate(0, 3, "%s掌握有点离谱,多想群友讨教讨教"));

        list.add(new Evaluate(3, 4, "%s掌握还ok,努力再上一层楼"));
        list.add(new Evaluate(3, 4, "%s掌握还不错,带飞下群友吧"));
        list.add(new Evaluate(3, 4, "%s掌握好挺好的,分享下学习心得呗"));
        list.add(new Evaluate(3, 4, "%s掌握的不赖,不能太飘"));

        list.add(new Evaluate(4, Integer.MAX_VALUE, "%s掌握的很好,以后就看你的了"));
        list.add(new Evaluate(4, Integer.MAX_VALUE, "%s掌握的很6,是个天才"));
        list.add(new Evaluate(4, Integer.MAX_VALUE, "%s掌握太熟了,未来可期"));
        list.add(new Evaluate(4, Integer.MAX_VALUE, "%s掌握太猛,不愧是顶级程序"));
        return list;
    }

    @Data
    @AllArgsConstructor
    private static class Evaluate {
        private double min;
        private double max;
        private String desc;
    }

    public static String avgEvaluate(double score) {
        if (score >= 0 && score < 3) {
            return "掌握较差";
        } else if (score >= 3 && score < 4) {
            return "掌握程度 ok";
        }
        return "掌握程度很好";
    }

    public static String evaluate(double score) {
        List<Evaluate> list = evaluates.stream().filter(item -> score >= item.getMin() && score < item.getMax()).collect(Collectors.toList());
        return list.get(RandomUtils.nextInt(0, list.size())).getDesc();
    }

}
