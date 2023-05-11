package com.jewel.usermanager.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.jewel.usermanager.configuration.logging.LogUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.FileReader;
import java.io.ObjectStreamException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@ControllerAdvice
@EnableWebMvc
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    public static JSONObject getJsonObject() {
        JSONObject jsonObject = null;
        try {
            FileReader fileReader = new FileReader("src/main/resources/controllerExceptionHandlerJson.json");
            jsonObject = (JSONObject) new JSONParser().parse(fileReader);
            fileReader.close();
        } catch (Exception e) {
            LOG.info(e.getMessage(), e.getCause());
        }
        return jsonObject;
    }

    public static String getTime() {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        String strDate = dateFormat.format(date);
        return strDate;
    }

    public static ErrorMessage errorBody(int statusCode, String date, String Description, String errorMessage) {
        ErrorMessage message = new ErrorMessage(statusCode, date, Description, errorMessage);
        return message;
    }

    @ExceptionHandler({DataAccessException.class, SQLException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorMessage> daoException(Exception ex, WebRequest request) throws Exception {
        if (ex instanceof DataAccessException) {
            if ((ex.getMessage().contains("Connection") || ex.getMessage().contains("connection")) && (ex.getMessage().contains("closed") || (ex.getMessage().contains("close")))) {
                LOG.error("{},{}", ex.getClass().getName(), ex.getMessage());
                ResponseEntity<ErrorMessage> errorMessage = new ResponseEntity<>(errorBody(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("DataAccessException")).get("statusCode")), getTime(), request.getDescription(false), "Connection Handle Already Closed"), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("DataAccessException")).get("responseStatus")));
                LogUtils.responseErrorLogs(errorMessage);
                return errorMessage;
            }
            if (ex.getMessage().contains("SQL syntax")) {
                LOG.error("{},{}", ex.getClass().getName(), ex.getMessage());
                ResponseEntity<ErrorMessage> errorMessage = new ResponseEntity<>(errorBody(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("DataAccessException")).get("statusCode")), getTime(), request.getDescription(false), "Error in SQL Syntax "), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("DataAccessException")).get("responseStatus")));
                LogUtils.responseErrorLogs(errorMessage);
                return errorMessage;
            }
            LOG.error("{},{}", ex.getClass().getName(), ex.getMessage());
            ResponseEntity<ErrorMessage> errorMessage = new ResponseEntity<>(errorBody(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("DataAccessException")).get("statusCode")), getTime(), request.getDescription(false), "DataAccessException has been occurred"), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("DataAccessException")).get("responseStatus")));
            LogUtils.responseErrorLogs(errorMessage);
            return errorMessage;
        }  else if (ex instanceof SQLException) {
            LOG.error("{},{}", ex.getClass().getName(), ex.getMessage());
            ResponseEntity<ErrorMessage> errorMessage = new ResponseEntity<>(errorBody(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("SQLException")).get("statusCode")), getTime(), request.getDescription(false), "SQLException has been occurred"), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("SQLException")).get("responseStatus")));
            LogUtils.responseErrorLogs(errorMessage);
            return errorMessage;
        } else if (ex instanceof DataIntegrityViolationException) {
            LOG.error("{},{}", ex.getClass().getName(), ex.getMessage());
            ResponseEntity<ErrorMessage> errorMessage = new ResponseEntity<>(errorBody(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("DataIntegrityViolationException")).get("statusCode")), getTime(), request.getDescription(false), getErrorMessage(ex.getMessage())), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("DataIntegrityViolationException")).get("responseStatus")));
            LogUtils.responseErrorLogs(errorMessage);
            return errorMessage;
        } else {
            LOG.error("{},{}", ex.getClass().getName(), ex.getMessage());
            throw ex;
        }
    }

    public String getErrorMessage(String errorMessage) {
        String[] words = errorMessage.split(":");
        String exceptedError = words[words.length - 1].trim();
        return exceptedError;
    }

    @ExceptionHandler({ObjectStreamException.class})
    public ResponseEntity<Object> handleObjectStreamException(
            final ConstraintViolationException ex, final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        final ErrorMessage apiError =
                new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("ObjectStreamException")).get("statusCode")), getTime(), request.getDescription(false), "exception in serialization process");
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("ObjectStreamException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request) {
        LOG.error("{},{}", ex.getClass().getName(), ex.getMessage());
        final List<String> errors = new ArrayList<String>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }
        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("MethodArgumentNotValidException")).get("statusCode")), getTime(), request.getDescription(false), errors);
        ResponseEntity<Object> errorResponse = handleExceptionInternal(ex, apiError, headers, HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("MethodArgumentNotValidException")).get("responseStatus")), request);
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;

    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            final MissingServletRequestParameterException ex, final HttpHeaders headers,
            final HttpStatus status, final WebRequest request) {

        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        String error = ex.getParameterName() + " parameter is missing";
        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("MissingServletRequestParameterException")).get("statusCode")), getTime(), request.getDescription(false), error);
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("MissingServletRequestParameterException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;

    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            final MethodArgumentTypeMismatchException ex, final WebRequest request) {

        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        String error = ex.getName() + " should be of type " + ex.getRequiredType().getName();
        final ErrorMessage apiError =
                new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("MethodArgumentTypeMismatchException")).get("statusCode")), getTime(), request.getDescription(false), error);
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("MethodArgumentTypeMismatchException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolation(
            final ConstraintViolationException ex, final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        List<String> errors = new ArrayList<String>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(violation.getRootBeanClass().getName() + " " +
                    violation.getPropertyPath() + ": " + violation.getMessage());
        }
        final ErrorMessage apiError =
                new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("ConstraintViolationException")).get("statusCode")), getTime(), request.getDescription(false), String.valueOf(errors));
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("ConstraintViolationException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            final NoHandlerFoundException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        String error = "No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL();
        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("NoHandlerFoundException")).get("statusCode")), getTime(), request.getDescription(false), error);
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("NoHandlerFoundException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;
    }

    @Override
    protected ResponseEntity<Object> handleBindException(final BindException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        final List<String> errors = new ArrayList<String>();
        for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (final ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }
        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("BindException")).get("statusCode")), getTime(), request.getDescription(false), errors);
        ResponseEntity<Object> errorResponse = handleExceptionInternal(ex, apiError, headers, HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("BindException")).get("responseStatus")), request);
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;

    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(final TypeMismatchException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        final String error = ex.getValue() + " value for " + ex.getPropertyName() + " should be of type " + ex.getRequiredType();

        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("TypeMismatchException")).get("statusCode")), getTime(), request.getDescription(false), error);
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("TypeMismatchException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            final HttpRequestMethodNotSupportedException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        StringBuilder builder = new StringBuilder();
        builder.append(ex.getMethod());
        builder.append(
                " method is not supported for this request. Supported methods are ");
        ex.getSupportedHttpMethods().forEach(t -> builder.append(t + " "));

        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("HttpRequestMethodNotSupportedException")).get("statusCode")), getTime(), request.getDescription(false)
                , builder.toString());
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("HttpRequestMethodNotSupportedException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;

    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(final HttpMediaTypeNotSupportedException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        StringBuilder builder = new StringBuilder();
        builder.append(ex.getContentType());
        builder.append(" media type is not supported. Supported media types are ");
        ex.getSupportedMediaTypes().forEach(t -> builder.append(t + ", "));

        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("HttpMediaTypeNotSupportedException")).get("statusCode")), getTime(), request.getDescription(false)
                , builder.substring(0, builder.length() - 2));
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("HttpMediaTypeNotSupportedException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;

    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            final ServletRequestBindingException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("ServletRequestBindingException")).get("statusCode")), getTime(), request.getDescription(false)
                , ex.getLocalizedMessage());
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("ServletRequestBindingException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;

    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("HttpMessageNotReadableException")).get("statusCode")), getTime(), request.getDescription(false)
                , "Type of request body is not in right format");
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("HttpMessageNotReadableException")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;

    }

    @ExceptionHandler({InvalidFormatException.class})
    public ResponseEntity<Object> handleInvalidFormatException(final InvalidFormatException ex,
                                                               final WebRequest request) {
        LOG.error("{},{}", ex.getClass().getName(), ex.getMessage());
        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("Exception")).get("statusCode")), getTime(), request.getDescription(false), "InvalidFormat");
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("Exception")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;
    }


    @ExceptionHandler({NullPointerException.class})
    public ResponseEntity<Object> handleNullPointerException(final NullPointerException ex,
                                                             final WebRequest request) {
        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        LOG.error("error", ex);
        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("Exception")).get("statusCode")), getTime(), request.getDescription(false), "Object cannot be null");
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("Exception")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAll(final Exception ex, final WebRequest request) {

        LOG.info("{},{}", ex.getClass().getName(), ex.getMessage());
        final ErrorMessage apiError = new ErrorMessage(Math.toIntExact((Long) ((JSONObject) getJsonObject().get("Exception")).get("statusCode")), getTime(), request.getDescription(false), "error occurred");
        ResponseEntity<Object> errorResponse = new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.valueOf((String) ((JSONObject) getJsonObject().get("Exception")).get("responseStatus")));
        LogUtils.responseErrorLogs(errorResponse);
        return errorResponse;
    }
}
