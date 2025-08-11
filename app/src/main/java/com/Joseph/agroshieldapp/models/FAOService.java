package com.Joseph.agroshieldapp.models;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FAOService {
    private static final String BASE_URL = "http://www.fao.org/";
    private static Retrofit retrofit = null;

    public static FAOApi getApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(FAOApi.class);
    }
}