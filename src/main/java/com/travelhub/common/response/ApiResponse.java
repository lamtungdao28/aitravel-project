package com.travelhub.common.response;
public record ApiResponse<T>(String message, T data) {}
