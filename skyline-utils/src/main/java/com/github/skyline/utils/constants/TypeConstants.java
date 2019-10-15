package com.github.skyline.utils.constants;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author luoxiangnan
 * @date 2019-10-15
 */
public class TypeConstants {

    /**
     * java基础类型
     */
    public static final Set<Class> BASIC_TYPE = Stream.of(
            char.class, Character.class,
            boolean.class, Boolean.class,
            short.class, Short.class,
            int.class, Integer.class,
            float.class, Float.class,
            double.class, Double.class,
            long.class, Long.class,
            String.class).collect(Collectors.toSet());
}
