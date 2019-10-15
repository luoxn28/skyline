package com.github.skyline.utils.common;

import com.github.skyline.utils.BeanCopyUtils;
import lombok.Data;
import org.junit.Test;

/**
 * @author luoxiangnan
 * @date 2019-10-15
 */
public class BeanCopyUtilsTest {

    @Data
    static class A {
        private String name;
        private C c;
        private Long length;
    }

    @Data
    static class B {
        private String name;
        private C c;
        private Long length;
    }

    @Data
    static class C {
        private String ccc;
    }

    @Test
    public void generateBeanCopyCode() {
        System.out.println(BeanCopyUtils.generateBeanCopyCodeV2(A.class, B.class));
    }
}
