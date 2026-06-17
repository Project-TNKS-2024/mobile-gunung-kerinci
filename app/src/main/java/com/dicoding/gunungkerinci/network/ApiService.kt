package com.dicoding.gunungkerinci.network

import com.dicoding.gunungkerinci.model.BaseResponse
import com.dicoding.gunungkerinci.model.CountryResponse
import com.dicoding.gunungkerinci.model.DestinasiResponse
import com.dicoding.gunungkerinci.model.DetailDestinasiResponse
import com.dicoding.gunungkerinci.model.ForgotPasswordRequest
import com.dicoding.gunungkerinci.model.GantiPasswordRequest
import com.dicoding.gunungkerinci.model.GoogleRedirectResponse
import com.dicoding.gunungkerinci.model.KabupatenResponse
import com.dicoding.gunungkerinci.model.KecamatanResponse
import com.dicoding.gunungkerinci.model.LoginRequest
import com.dicoding.gunungkerinci.model.LoginResponse
import com.dicoding.gunungkerinci.model.PendakiIdentityResponse
import com.dicoding.gunungkerinci.model.ProfileResponse
import com.dicoding.gunungkerinci.model.ProvinsiResponse
import com.dicoding.gunungkerinci.model.RegisterRequest
import com.dicoding.gunungkerinci.model.RegisterResponse
import com.dicoding.gunungkerinci.model.ResetPasswordRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("api/register")
    @Headers(
        "Accept: application/json"
    )
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("api/email/resend")
    @Headers(
        "Accept: application/json"
    )
    suspend fun resendEmailVerification(): Response<BaseResponse<Unit>>

    @POST("api/login")
    @Headers(
        "Accept: application/json"
    )
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/forgot-password")
    @Headers(
        "Accept: application/json"
    )
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<BaseResponse<Unit>>

    @POST("api/reset-password")
    @Headers(
        "Accept: application/json"
    )
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<BaseResponse<Unit>>

    @GET("api/profile/getbiodata")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ProfileResponse>


    // UPDATE PROFILE
    @Multipart
    @POST("api/profile/updatebiodata")
    @Headers(
        "Accept: application/json"
    )
    suspend fun updateProfile(
        @Header("Authorization") token: String,

        @Part("firstName") firstName: RequestBody,
        @Part("lastName") lastName: RequestBody,
        @Part("kewarganegaraan") nationality: RequestBody,

        @Part("jenis_kelamin") gender: RequestBody,
        @Part("tanggal_lahir") birthDate: RequestBody,
        @Part("nik") nik: RequestBody,

        @Part("nomor_telepon") phone: RequestBody,
        @Part("telp_country") phoneCountry: RequestBody,

        @Part("provinsi") provinsi: RequestBody?,
        @Part("kabupaten_kota") kabupaten: RequestBody?,
        @Part("kecamatan") kecamatan: RequestBody?,

        @Part lampiran_identitas: MultipartBody.Part?
    ): Response<BaseResponse<Unit>>


    // GET NEGARA
    @GET("api/domisili/negara")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getNegara(): Response<CountryResponse>

    // ================= DOMISILI =================
    @GET("api/domisili/provinsi")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getProvinsi(): Response<ProvinsiResponse>

    @GET("api/domisili/provinsi/{id}")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getProvinsiById(@Path("id") id: Int): Response<ProvinsiResponse>

    @GET("api/domisili/kabupaten/provinsi/{id}")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getKabupatenByProvinsi(@Path("id") provinsiId: Int): Response<KabupatenResponse>

    @GET("api/domisili/kabupaten/{id}")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getKabupatenById(@Path("id") id: Int): Response<KabupatenResponse>

    @GET("api/domisili/kecamatan/kabupaten/{id}")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getKecamatanByKabupaten(@Path("id") kabupatenId: Int): Response<KecamatanResponse>

    @GET("api/domisili/kecamatan/{id}")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getKecamatanById(@Path("id") id: Int): Response<KecamatanResponse>

    @POST("api/logout")
    @Headers(
        "Accept: application/json"
    )
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<BaseResponse<Unit>>

    @POST("api/profile/gantipassword")
    @Headers("Content-Type: application/json")
    suspend fun gantiPassword(
        @Header("Authorization") token: String,
        @Body body: GantiPasswordRequest
    ): Response<BaseResponse<Unit>>

    @GET("api/auth/google/redirect")
    suspend fun googleRedirect(): Response<BaseResponse<GoogleRedirectResponse>>

    @GET("api/profile/pendaki-identity")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getPendakiIdentity(
        @Header("Authorization") token: String
    ): Response<PendakiIdentityResponse>

    @GET("api/destinasi")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getDestinasi(): Response<DestinasiResponse>

    @GET("api/destinasi/{id}")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getDetailDestinasi(
        @Path("id") id: Int
    ): Response<DetailDestinasiResponse>
}