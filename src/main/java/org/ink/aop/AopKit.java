package org.ink.aop;

import org.ink.aop.annotation.Before;
import org.ink.web.http.Request;
import org.ink.web.http.Response;
import org.ink.web.route.Route;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * set args in Aop methods
 *
 * @author zhuyichen
 */
class AopKit {

     static void argsSetter(ProxyEntity proxyEntity, Route route, Request request, Response response) {

        Method method = proxyEntity.getProxyMethod();

        Object[] proxyObjects = proxyEntity.getParameters();
        Object[] routeObjects = route.getParamters();
        Parameter[] routeParameters = route.getMethod().getParameters();
        Parameter[] proxyParameters = method.getParameters();
        assert routeObjects.length == routeParameters.length;

        //response request setter
        int index = 0;
        int count = 0;
        for (Parameter p : proxyParameters) {
            if (p.getType().equals(Response.class)) {
                proxyObjects[index] = response;
                count++;
            }
            else if (p.getType().equals(Request.class)) {
                proxyObjects[index] = request;
                count++;
            }
            index++;
        }

        //如果只有response和request参数
        if (count == index) {
            return;
        }

        //如果还有其他的参数，再继续
        String[] args = method.getAnnotation(Before.class).args().split(",");
        //arg -> route.object[i]
        Map<String, Object> map = new HashMap<>();
        index = 0;
        for (Parameter p : routeParameters) {
            for (String s : args) {
                if (s.equals(p.getName())) {
                    map.put(s, routeObjects[index]);
                }
            }
            index++;
        }

        index = 0;
        for (Parameter p : proxyParameters) {
            if (map.containsKey(p.getName())) {
                proxyObjects[index] = map.get(p.getName());
            }
            index++;
        }
    }

}
