package com.github.skyline.utils;

import com.github.skyline.utils.constants.TypeConstants;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 实例toString格式转对应实例
 * 注意：
 *  1. 实例类需要有默认构造方法
 *  2. 如果断言在运行时没有起作用，在VM参数中增加 -ea
 *
 * @author luoxiangnan
 * @date 2019-10-15
 */
public class ToStringConvertUtils {

    @SuppressWarnings("all")
    public static  <T> T toString2Object(Class<T> clazz, String toString) throws Exception {
//        assert  normalFormat = StringUtils.isNotBlank(toString) &&
//                (toString = toString.trim()).startsWith(clazz.getSimpleName() + "(") &&
//                toString.endsWith(")");
        if (TypeConstants.BASIC_TYPE.contains(clazz)) {
            // 基本类型直接进行解析toString
            return (T) basicTypeValue(clazz, toString);
        }

        T result = clazz.newInstance();
        try {
            toString = toString.substring(toString.indexOf("(") + 1, toString.lastIndexOf(")"));
            while (StringUtils.isNotEmpty(toString)) {
                String token = splitToken(toString);
                if (StringUtils.isBlank(token)) {
                    break;
                }
                toString = StringUtils.removeStart(StringUtils.removeStart(toString, token).trim(), ",").trim();

                // 解析token并设置值
                assert token.contains("=");
                String fieldName = StringUtils.substringBefore(token, "=").trim();
                String fieldValue = StringUtils.substringAfter(token, "=").trim();
                if (StringUtils.isBlank(fieldValue) || StringUtils.equalsIgnoreCase(fieldValue, "null")) {
                    continue;
                }

                Field field = ReflectionUtils.getField(result, fieldName);
                assert field != null;
                ReflectionUtils.setField(result, fieldName, fieldValue2Object(field, fieldValue));
            }
        } catch (Exception e) {
            System.err.println(String.format("clazz=%s toString=%s", clazz, toString));
            throw e;
        }
        return result;
    }

    private static Object fieldValue2Object(Field field, String fieldValue) throws Exception {
        if (TypeConstants.BASIC_TYPE.contains(field.getType())) {
//        if (field.getType().isPrimitive()) {
            return basicTypeValue(field.getType(), fieldValue);
        } else if (field.getType().isArray() || field.getType().isAssignableFrom(Array.class) || field.getType().isAssignableFrom(List.class)) {
            return listTypeValue(field, fieldValue);
        } else if (field.getType().isAssignableFrom(Map.class)) {
            return mapTypeValue(field, fieldValue);
        } else if (field.getType().isAssignableFrom(Date.class)) {
            return new Date(fieldValue);
        } else {
            return toString2Object(field.getType(), fieldValue);
        }
    }

    private static Object basicTypeValue(Type type, String value) {
        if (type == Character.class || type == char.class) {
            return value.charAt(0);
        } else if (type == Boolean.class || type == boolean.class) {
            return Boolean.valueOf(value);
        } else if (type == Short.class || type == short.class) {
            return Short.valueOf(value);
        } else if (type == Integer.class || type == int.class) {
            return Integer.valueOf(value);
        } else if (type == Float.class || type == float.class) {
            return Float.valueOf(value);
        } else if (type == Double.class || type == double.class) {
            return Double.valueOf(value);
        }else if (type == Long.class || type == long.class) {
            return Long.valueOf(value);
        } else if (type == String.class) {
            return value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List listTypeValue(Field field, String fieldValue) throws Exception {
        fieldValue = StringUtils.removeStart(fieldValue, "[");
        fieldValue = StringUtils.removeEnd(fieldValue, "]");

        String token = "";
        List<Object> result = new ArrayList<>();
        while (StringUtils.isNotEmpty(token = splitToken(fieldValue))) {
            fieldValue = StringUtils.removeStart(StringUtils.removeStart(fieldValue, token).trim(), ",").trim();
            result.add(toString2Object((Class) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0], token));
        }
        return result;
    }

    @SuppressWarnings("all")
    private static Map mapTypeValue(Field field, String toString) throws Exception {
        toString = StringUtils.removeStart(toString, "{");
        toString = StringUtils.removeEnd(toString, "}");

        String token = "";
        Map result = new HashMap();
        while (StringUtils.isNotEmpty(token = splitToken(toString))) {
            toString = StringUtils.removeStart(StringUtils.removeStart(toString, token).trim(), ",").trim();
            assert token.contains("=");
            String fieldName = StringUtils.substringBefore(token, "=").trim();
            String fieldValue = StringUtils.substringAfter(token, "=").trim();

            assert TypeConstants.BASIC_TYPE.contains(((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
            result.put(basicTypeValue(((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0], fieldName),
                    toString2Object((Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1], fieldValue));

        }
        return result;
    }

    /**
     * 获取第一个token，注意: toString不再包括最外层的()
     */
    private static Set<Character> BRACKET_LEFT  = Stream.of('(', '{', '[').collect(Collectors.toSet());
    private static Set<Character> BRACKET_RIGHT = Stream.of(')', '}', ']').collect(Collectors.toSet());
    private static String splitToken(String toString) {
        if (StringUtils.isBlank(toString)) {
            return toString;
        }

        int bracketNum = 0;
        for (int i = 0; i < toString.length(); i++) {
            char c = toString.charAt(i);
            if (BRACKET_LEFT.contains(c)) {
                bracketNum++;
            } else if (BRACKET_RIGHT.contains(c)) {
                bracketNum--;
            } else if ((c == ',') && (bracketNum == 0)) {
                return toString.substring(0, i);
            }
        }
        assert bracketNum == 0;
        return toString;
    }
}
