package com.joelwhissel.personalstocktracker.data.repository;

public interface Callback<T> {
    void onResult(Resource<T> result);
}
