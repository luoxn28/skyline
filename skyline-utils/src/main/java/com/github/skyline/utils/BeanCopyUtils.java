package com.github.skyline.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author luoxiangnan
 * @date 2019-10-15
 */
public class BeanCopyUtils {

    public static String generateBeanCopyCodeV2(Class source, Class target) {
        List<String> list = generateBeanCopyCode(source, target);
        StringBuilder result = new StringBuilder();
        list.forEach(result::append);
        return result.toString();
    }

    public static List<String> generateBeanCopyCode(Class source, Class target) {
        List<String> result = doGenerateBeanCopyCode(source, "source", target,1);
        result.add(0, "{\n");
        result.add("}\n");
        return result;
    }

    private static List<String> doGenerateBeanCopyCode(Class sourceClass, String sourceName, Class targetClass, int level) {
        // 获取field属性名列表
        Set<String> sourceFieldNameList = Arrays.stream(sourceClass.getDeclaredFields()).peek(o -> o.setAccessible(true))
                .map(Field::getName).collect(Collectors.toSet());
        List<Field> targetFieldNameList = Arrays.stream(targetClass.getDeclaredFields()).peek(o -> o.setAccessible(true))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(targetFieldNameList)) {
            return new ArrayList<>();
        }

        String targetName = "target";
        List<String> result = new ArrayList<>();
        result.add(String.format("\t%s %s = new %s();\n", targetClass.getSimpleName(), targetName,
                targetClass.getSimpleName()));
        targetFieldNameList.forEach(field -> {
            String filedName = field.getName();
            if (sourceFieldNameList.contains(filedName)) {
                result.add(String.format("%s%s.set%s(%s.get%s());\n", "\t", targetName, StringUtils.capitalize(filedName), sourceName, StringUtils.capitalize(filedName)));
            } else {
                result.add(String.format("%s%s.set%s(null);\n", "\t", targetName, StringUtils.capitalize(filedName)));
            }
        });
        return result;
    }
}
