package com.abhout.cortex_app_be.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T>{
    private Status status;
    private T data;
    private ErrorDetails errorDetails;

    public static<T> ApiResponse<T> success(T data){
        return new ApiResponse<>(Status.SUCCESS,data,null);
    }

    public static<T> ApiResponse<T> success(){
        return new ApiResponse<>(Status.SUCCESS,null,null);
    }

    public static<T> ApiResponse<T> error(String code, String message, String requestId) {
        return new ApiResponse<>(Status.ERROR, null, new ErrorDetails(code, message, requestId));
    }

    public static<T> ApiResponse<T> error(ErrorDetails errorDetails) {
        return new ApiResponse<>(Status.ERROR, null, errorDetails);
    }
}
