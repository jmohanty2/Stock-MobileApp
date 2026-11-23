package com.joelwhissel.personalstocktracker.data.repository;

import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

public class Resource<T> {
    public enum Status { LOADING, SUCCESS, ERROR }
    public final Status status;
    public final T data;
    public final ErrorType error;

    private Resource(Status s, T d, ErrorType e) {
        status = s;
        data = d;
        error = e;
    }

    public static <T> Resource<T> success(T d) {
        return new Resource<>(Status.SUCCESS, d, null);
    }

    public static <T> Resource<T> error(ErrorType e) {
        return new Resource<>(Status.ERROR, null, e);
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null);
    }
}

