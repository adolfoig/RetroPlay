package com.example.retroplay.Supebase;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface SupabaseStorageApi {

    @Multipart
    @POST("storage/v1/object/{bucket}/{fileName}")
    Call<Void> uploadImage(
            @Header("Authorization") String authToken,
            @Path("bucket") String bucket,
            @Path("fileName") String fileName,
            @Part MultipartBody.Part file
    );

    @DELETE("storage/v1/object/{bucket}/{fileName}")
    Call<Void> deleteImage(
            @Header("Authorization") String authToken,
            @Path("bucket") String bucket,
            @Path("fileName") String fileName
    );


}