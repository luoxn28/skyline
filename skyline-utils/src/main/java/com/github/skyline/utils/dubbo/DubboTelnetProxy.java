package com.github.skyline.utils.dubbo;

import com.alibaba.fastjson.JSON;
import com.sun.tools.javac.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * dubbo telnet proxy
 *
 * @author luoxiangnan
 * @date 2019-10-14
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DubboTelnetProxy implements MethodInterceptor {
    private final static String DEFAULT_METHOD = "toString";

    private String ip;
    private Integer port;

    @Override
    public Object intercept(Object obj, Method method, Object[] params, MethodProxy proxy) throws Throwable {
        if (DEFAULT_METHOD.equals(method.getName())) {
            return obj.getClass().getName();
        }

        TelnetClient telnetClient = new TelnetClient();
        telnetClient.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
        telnetClient.connect(ip, port);
        try {
            InputStream in = telnetClient.getInputStream();
            PrintStream out = new PrintStream(telnetClient.getOutputStream());

            // 1. 发送dubbo telnet请求
            StringBuffer request = new StringBuffer("invoke ");
            request.append(method.getDeclaringClass().getTypeName()).append(".");
            request.append(method.getName()).append("(");
            request.append(StringUtils.join(List.from(params).stream().map(JSON::toJSONString).collect(Collectors.toList()), ",")).append(")");
            out.println(request.toString());
            out.flush();

            // 2. 结果处理
            int len = 0;
            byte[] buffer = new byte[512];
            String result = "";
            while (!result.contains(StringUtils.LF) && (len = in.read(buffer)) > 0) {
                result += new String(ArrayUtils.subarray(buffer, 0, len));
            }
            result = StringUtils.substringBefore(result, StringUtils.LF);
            if (StringUtils.isBlank(result) || !result.startsWith("{")) {
                throw new RuntimeException(result);
            }

            // 3. 反序列化
            return JSON.parseObject(result, method.getGenericReturnType());
        } finally {
            telnetClient.disconnect();
        }
    }

    /**
     * dubbo telnet建造者
     */
    public static class Builder {
        final static String DEFAULT_IP = "127.0.0.1";
        final static Integer DEFAULT_PORT = 20881;

        /**
         * 创建dubbo telnet代理
         */
        public static <T> T enhance(Class<T> object) {
            return enhance(object, null, null);
        }
        public static <T> T enhance(Class<T> object, String ip) {
            return enhance(object, ip, DEFAULT_PORT);
        }
        public static <T> T enhance(Class<T> object, Integer port) {
            return enhance(object, null, port);
        }

        public static <T> T enhance(Class<T> object, String ip, Integer port) {
            // 如果IP为空，尝试从properties解析ip地址
            ip = StringUtils.defaultIfBlank(ip, BuilderPropertiesUtil.parseIpFromProperties(object.getName()));

            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(object);
            enhancer.setCallback(new DubboTelnetProxy(ObjectUtils.defaultIfNull(ip, DEFAULT_IP), ObjectUtils.defaultIfNull(port, DEFAULT_PORT)));
            return (T) enhancer.create();
        }
    }

    /**
     * dubbo telnet构建辅助类
     */
    private static class BuilderPropertiesUtil {
        private final static String MOCK_PREFIX = System.getProperty("skyline.mock.prefix", "mock.dubbo.");
        private final static String MOCK_CLASS_PREFIX = System.getProperty("skyline.mock.class.prefix", "com.github.");

        /**
         * 从properties中解析服务对应IP地址，最长匹配原则
         */
        public static String parseIpFromProperties(String classPath) {
            if (StringUtils.isBlank(classPath) || !classPath.startsWith(MOCK_CLASS_PREFIX)) {
                return null;
            }

            classPath = MOCK_PREFIX + classPath;
            Set<String> ipPropertiesNameSet = System.getProperties().stringPropertyNames().stream()
                    .filter(o -> o.startsWith(MOCK_PREFIX + MOCK_CLASS_PREFIX)).collect(Collectors.toSet());
            do {
                if (ipPropertiesNameSet.contains(classPath)) {
                    return System.getProperty(classPath);
                }
            } while (StringUtils.isNotBlank(classPath = removeLastToken(classPath)));
            return null;
        }

        /**
         * 移除类路径最后一个"."及其后面字符串
         */
        private static String removeLastToken(String classPath) {
            return StringUtils.isNotBlank(classPath) && classPath.contains(".") ?
                    StringUtils.substringBeforeLast(classPath, ".") : null;
        }
    }
}
