package com.github.skyline.utils.common;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

/**
 * @author luoxiangnan
 * @date 2019-10-15
 */
public class ReflectionUtils {

    public static <T> Field getField(@NonNull T target, @NonNull String fieldName) {
        return org.springframework.util.ReflectionUtils.findField(target instanceof Class ? (Class) target : target.getClass(), fieldName);
    }

    public static Object getFieldValue(@NonNull Object object, @NonNull String fieldName) throws IllegalAccessException {
        assert !(object instanceof Class);
        Field field = getField(object, fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

    /**
     * 使用value类名的uncapitalize格式设置对应属性值
     */
    public static <T> T setField(@NonNull T object, @NonNull Object value) {
        return setField(object, StringUtils.uncapitalize(value.getClass().getSimpleName()), value);
    }

    public static <T> T setField(@NonNull T object, @NonNull String fieldName, @NonNull Object value) {
        Field field = org.springframework.util.ReflectionUtils.findField(object.getClass(), fieldName);
        return setField(object, field, value);
    }

    public static <T> T setField(@NonNull T object, @NonNull Field field, @NonNull Object value) {
        field.setAccessible(true);
        org.springframework.util.ReflectionUtils.setField(field, object, value);
        return object;
    }
}
