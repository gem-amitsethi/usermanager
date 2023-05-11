package com.jewel.usermanager.configuration.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class LogUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LogUtils.class);

    static Date date = Calendar.getInstance().getTime();

    public static void requestLog(HttpServletRequest request)  {
        CustomRequestLog requestLog = new CustomRequestLog();
        requestLog.setTimeStamp(date);
        requestLog.setRequestURL(String.valueOf(request.getRequestURL()));
        requestLog.setRequestType(request.getMethod());
        requestLog.setEndPoint(request.getRequestURI());
        Map<String, String> map = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }
        requestLog.setHeaders(String.valueOf(map));
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        LOG.error("{}",gson.toJson(requestLog));
    }
    public static void requestLog(HttpServletRequest request,Object payload){
        CustomRequestLog requestLog = new CustomRequestLog();
        requestLog.setTimeStamp(date);
        requestLog.setRequestURL(String.valueOf(request.getRequestURL()));
        requestLog.setRequestType(request.getMethod());
        requestLog.setEndPoint(request.getRequestURI());
        requestLog.setPayLoad(payload);
        Map<String, String> map = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }
        requestLog.setHeaders(String.valueOf(map));
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        LOG.error("{}",gson.toJson(requestLog));
    }

    public static void responseLog(ResponseEntity responseBody){
        CustomResponseLog responseLog = new CustomResponseLog();
        responseLog.setTimeStamp(date);
        responseLog.setHttpStatusCode(responseBody.getStatusCodeValue());
        responseLog.setResponse(responseBody.getBody());
        LOG.info("{}",new Gson().toJson(responseLog));
    }

    public static void responseErrorLogs(ResponseEntity responseError){
        CustomResponseLog responseLog = new CustomResponseLog();
        responseLog.setHttpStatusCode(responseError.getStatusCodeValue());
        responseLog.setResponse(responseError.getBody());
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        LOG.error("{}",gson.toJson(responseLog));
    }

}