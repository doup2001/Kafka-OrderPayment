package lecture.fastcampus.ecommerce.core.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties("error-trace")
@RestControllerAdvice
@Slf4j
public class GlobalException extends ResponseEntityExceptionHandler {

    private boolean stackTrace;

    // 유효성 검사가 틀렸을 때, 나오는 예외처리이다.
    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logger.error("BAD_REQUEST ::: ", ex);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(null, ex.getBindingResult().getAllErrors().get(0).getDefaultMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public final ErrorResponse handleIOExceptions(Exception ex, WebRequest request) {
        List<StackTraceElement> stackTraces = null;
        if (stackTrace) {
            stackTraces = Arrays.asList(ex.getStackTrace());
        }
        logger.error("ERROR ::: [IOException] ", ex);
        return new ErrorResponse(stackTraces, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public final ErrorResponse handleAllExceptions(Exception ex, WebRequest request) {
        List<StackTraceElement> stackTraces = null;
        if (stackTrace) {
            stackTraces = Arrays.asList(ex.getStackTrace());
        }
        logger.error("ERROR ::: [AllException] ", ex);
        return new ErrorResponse(stackTraces, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
