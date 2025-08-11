package com.Joseph.agroshieldapp.models;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FAOApi {
    @GET("faostat/api/v1/en/data")
    Call<FAOResponse> getCropDiseases(
            @Query("dataset") String dataset,
            @Query("crop") String crop,
            @Query("format") String format
    );
}