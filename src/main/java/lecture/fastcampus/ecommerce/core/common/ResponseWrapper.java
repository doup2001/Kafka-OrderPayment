package lecture.fastcampus.ecommerce.core.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
@Slf4j
public class ResponseWrapper implements ResponseBodyAdvice<Object> {

    /*
        AOP를 통해서 미리, 바꿔주는 가져오는 클래스이다.
     */

    // true로 반환해서 거쳐가도록
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        log.debug("supports method returnType: {}", returnType);
        log.debug("supports converterType: {}", converterType);
        return true;
    }

    // 가져온 걸 다시 클래스로 작성
    // 단점은 primitive 클래스를 반환 X
    // CommonHttpMessageConverter에서 해결한다.
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        if (body instanceof ErrorResponse) {
            return new ApiResponse<>("ERROR", body);
        }
        log.info("execute AOP - beforeBodyWrite");
        return new ApiResponse<>("SUCCESS", body);
    }



}
