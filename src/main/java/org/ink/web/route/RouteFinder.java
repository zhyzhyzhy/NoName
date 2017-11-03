package org.ink.web.route;


import com.alibaba.fastjson.JSON;
import org.ink.exception.UnauthorizedException;
import org.ink.security.JwtInfo;
import org.ink.security.SecurityManager;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import org.ink.web.annotation.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by zhuyichen on 2017/7/12.
 */
public class RouteFinder {

    //保存路由参数的Pattern和路由的map
    private static final Map<Pattern, Route> routeMap = new HashMap<>();


    //根据参数进行正则替换
    public static Pattern pathCompiler(String path, Method method) {
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (parameter.getAnnotations() == null) {
                continue;
            }
            Annotation annotation = parameter.getAnnotations()[0];
            if (annotation instanceof PathVariable) {
                //如果是字符串
                if (parameter.getType() == String.class) {
                    path = path.replace("{" + parameter.getName() + "}", "[0-9\\d\\D]*");
                }
                //如果是数字
                else if (parameter.getType() == Integer.class
                        || parameter.getType() == Long.class) {

                    path = path.replace("{" + parameter.getName() + "}", "[0-9]*");
                }

            }
        }
        return Pattern.compile(path);
    }

    public static void addRouter(Pattern pattern, Route route) {
        routeMap.put(pattern, route);
    }


    public static Route findRoute(FullHttpRequest fullHttpRequest) throws UnauthorizedException {

        String path = fullHttpRequest.uri();
        HttpMethod method = fullHttpRequest.method();


        //如果有params，找到问号的位置
        int splitIndex = path.indexOf('?');
        //如果没有问号，则index为请求uri的长度
        if (splitIndex == -1) {
            splitIndex = path.length();
        }
        //如果最后一个字符是'/',去掉
        if (path.charAt(splitIndex-1) == '/') {
            splitIndex--;
        }

        //遍历map
        for (Pattern pattern : routeMap.keySet()) {
            //如果找到uri匹配的
            if (pattern.matcher(path.substring(0, splitIndex)).matches()) {

                Route route = routeMap.get(pattern);

                //如果请求方式一样
                if (route.getHttpMethod().equals(method)) {

                    if (route.isSecurity()) {
                        JwtInfo jwtInfo = SecurityManager.check(fullHttpRequest, route);
                        if (!jwtInfo.isOk()) {
                            throw new UnauthorizedException();
                        }
                    }

                    //设置@PathVariable
                    routePathVariableSetter(path, route);

                    if (route.getHttpMethod().equals(HttpMethod.GET)) {
                        //设置GET @RequestParam
                        GETParamsSetter(path, route);
                    } else if (route.getHttpMethod().equals(HttpMethod.POST)) {
                        //设置POST @RequestParam
                        POSTParamsSetter(fullHttpRequest, route);
                    }

                    //设置@RequestJson
                    if ("application/json".equals(fullHttpRequest.headers().get("content-Type"))) {
                        routeRequestJsonSetter(fullHttpRequest.content().copy().toString(CharsetUtil.UTF_8), route);
                    }

                    return route;
                }
            }
        }
        return null;
    }

    //处理@RequestJson参数
    public static void routeRequestJsonSetter(String json, Route route) {
        Method method = route.getMethod();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Annotation[] annotations = parameters[i].getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof RequestJson) {
                    //得到参数的类
                    Class<?> class1 = parameters[i].getType();
                    Object object = JSON.parseObject(json, class1);
                    route.getParamters()[i] = object;
                }
            }
        }
    }


    //处理@PathVariable参数
    public static void routePathVariableSetter(String path, Route route) {
        Method method = route.getMethod();
        Annotation annotation = method.getAnnotations()[0];
        String uri = "";


        if (annotation instanceof GET) {
            uri = ((GET) annotation).value();
        } else if (annotation instanceof POST) {
            uri = ((POST) annotation).value();
        } else if (annotation instanceof PUT) {
            uri = ((PUT) annotation).value();
        } else if (annotation instanceof DELETE) {
            uri = ((DELETE) annotation).value();
        }

        String[] requestPaths = path.split("/");
        String[] originPath = uri.split("/");

        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < requestPaths.length && i < originPath.length; i++) {
            if (!requestPaths[i].equals(originPath[i])) {
                for (int j = 0; j < parameters.length; j++) {
                    if (parameters[j].getName().equals(originPath[i].substring(1, originPath[i].length() - 1))) {
                        //根据@PathVariable的类型进行转换
                        if (parameters[j].getType().equals(Integer.class)) {
                            route.getParamters()[j] = Integer.valueOf(requestPaths[i]);
                        } else if (parameters[j].getType().equals(Long.class)) {
                            route.getParamters()[j] = Long.valueOf(requestPaths[i]);
                        } else if (parameters[j].getType().equals(String.class)) {
                            route.getParamters()[j] = requestPaths[i];
                        }
                    }
                }
            }
        }
    }

    //处理@RequestParam参数
    public static void GETParamsSetter(String path, Route route) {

        QueryStringDecoder decoder = new QueryStringDecoder(path);
        Map<String, List<String>> map = decoder.parameters();
        Parameter[] parameters = route.getMethod().getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Annotation[] annotations = parameters[i].getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof RequestParam) {
                    List<String> list = map.get(parameters[i].getName());
                    if (list == null || list.size() == 0) {
                        route.getParamters()[i] = null;
                    } else {
                        route.getParamters()[i] = list.get(0);
                    }
                }
            }
        }
    }

    //处理@POSTParam参数
    public static void POSTParamsSetter(FullHttpRequest request, Route route) {
        Parameter[] parameters = route.getMethod().getParameters();


        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
        List<InterfaceHttpData> interfaceHttpData = decoder.getBodyHttpDatas();

        interfaceHttpData.stream()
                .forEach(interfaceHttpData1 -> {
                    //if is Attribute
                    if (interfaceHttpData1.getHttpDataType().equals(InterfaceHttpData.HttpDataType.Attribute)) {

                        Attribute attribute = (Attribute) interfaceHttpData1;

                        for (int i = 0; i < parameters.length; i++) {
                            Annotation[] annotations = parameters[i].getAnnotations();
                            for (Annotation annotation : annotations) {
                                if (annotation instanceof RequestParam) {
                                    String parameterName = parameters[i].getName();
                                    if (parameterName.equals(interfaceHttpData1.getName())) {
                                        try {
                                            route.getParamters()[i] = attribute.getValue();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
        }

}
