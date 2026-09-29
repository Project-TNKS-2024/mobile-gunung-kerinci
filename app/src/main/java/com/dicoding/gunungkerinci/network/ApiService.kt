package com.dicoding.gunungkerinci.network

import com.dicoding.gunungkerinci.model.BaseResponse
import com.dicoding.gunungkerinci.model.BerandaResponse
import com.dicoding.gunungkerinci.model.CheckpointQrRequest
import com.dicoding.gunungkerinci.model.CheckpointQrResponse
import com.dicoding.gunungkerinci.model.CancelCheckpointResponse
import com.dicoding.gunungkerinci.model.CountryResponse
import com.dicoding.gunungkerinci.model.DestinasiResponse
import com.dicoding.gunungkerinci.model.DetailDestinasiResponse
import com.dicoding.gunungkerinci.model.DisasterReportResponse
import com.dicoding.gunungkerinci.model.DisasterReportsListResponse
import com.dicoding.gunungkerinci.model.EmergencyActiveResponse
import com.dicoding.gunungkerinci.model.ForgotPasswordRequest
import com.dicoding.gunungkerinci.model.GantiPasswordRequest
import com.dicoding.gunungkerinci.model.GoogleRedirectResponse
import com.dicoding.gunungkerinci.model.GpsCheckRequest
import com.dicoding.gunungkerinci.model.GpsCheckResponse
import com.dicoding.gunungkerinci.model.KabupatenResponse
import com.dicoding.gunungkerinci.model.KecamatanResponse
import com.dicoding.gunungkerinci.model.LoginRequest
import com.dicoding.gunungkerinci.model.LoginResponse
import com.dicoding.gunungkerinci.model.ManualCheckInRequest
import com.dicoding.gunungkerinci.model.ManualCheckInResponse
import com.dicoding.gunungkerinci.model.MyPositionResponse
import com.dicoding.gunungkerinci.model.MyTiketResponse
import com.dicoding.gunungkerinci.model.PendakiIdentityResponse
import com.dicoding.gunungkerinci.model.ProfileResponse
import com.dicoding.gunungkerinci.model.ProvinsiResponse
import com.dicoding.gunungkerinci.model.RegisterRequest
import com.dicoding.gunungkerinci.model.RegisterResponse
import com.dicoding.gunungkerinci.model.ResetPasswordRequest
import com.dicoding.gunungkerinci.model.SosActiveResponse
import com.dicoding.gunungkerinci.model.SosCallOptionsResponse
import com.dicoding.gunungkerinci.model.SosMessagesResponse
import com.dicoding.gunungkerinci.model.SosSendMessageResponse
import com.dicoding.gunungkerinci.model.SosTriggerRequest
import com.dicoding.gunungkerinci.model.SosTriggerResponse
import com.dicoding.gunungkerinci.model.TrackingBatchData
import com.dicoding.gunungkerinci.model.TrackingBatchRequest
import com.dicoding.gunungkerinci.model.TrackingGpsData
import com.dicoding.gunungkerinci.model.TrackingPointRequest
import com.dicoding.gunungkerinci.model.TrackingPostResponse
import com.dicoding.gunungkerinci.model.TrackingProgressResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("api/beranda")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getBeranda(
        @Header("Authorization") token: String
    ): Response<BerandaResponse>

    @GET("api/destinasi/{id}")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getDetailDestinasi(
        @Path("id") id: Int
    ): Response<DetailDestinasiResponse>

    @POST("api/tracking/checkpoint/qr")
    @Headers(
        "Accept: application/json"
    )
    suspend fun checkInQr(
        @Header("Authorization") token: String,
        @Body request: CheckpointQrRequest
    ): Response<CheckpointQrResponse>

    /**
     * Batalkan kehadiran pada satu pos.
     * Server mengizinkan pendaki membatalkan miliknya sendiri, atau ketua tim
     * membatalkan milik anggota pada booking yang sama.
     */
    @DELETE("api/tracking/checkpoint/{checkpointLogId}")
    @Headers("Accept: application/json")
    suspend fun cancelCheckpoint(
        @Header("Authorization") token: String,
        @Path("checkpointLogId") checkpointLogId: Int
    ): Response<CancelCheckpointResponse>

    @POST("api/tracking/checkpoint/manual")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun checkInManual(
        @Header("Authorization") token: String,
        @Body request: ManualCheckInRequest
    ): Response<ManualCheckInResponse>

    @POST("api/tracking/checkpoint/gps")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun checkNearbyPostGps(
        @Header("Authorization") token: String,
        @Body request: GpsCheckRequest
    ): Response<GpsCheckResponse>

    @POST("api/tracking/gps")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun postTrackingGps(
        @Header("Authorization") token: String,
        @Body request: TrackingPointRequest
    ): Response<BaseResponse<TrackingGpsData>>

    @POST("api/tracking/gps/batch")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun postTrackingGpsBatch(
        @Header("Authorization") token: String,
        @Body request: TrackingBatchRequest
    ): Response<BaseResponse<TrackingBatchData>>

    @GET("api/tracking/my-position")
    @Headers("Accept: application/json")
    suspend fun getMyPosition(
        @Header("Authorization") token: String
    ): Response<MyPositionResponse>

    @GET("api/tracking/progress/{booking_id}")
    @Headers("Accept: application/json")
    suspend fun getTrackingProgress(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: String
    ): Response<TrackingProgressResponse>

    @GET("api/tracking/posts/{gate_id}")
    @Headers("Accept: application/json")
    suspend fun getTrackingPosts(
        @Header("Authorization") token: String,
        @Path("gate_id") gateId: Int
    ): Response<TrackingPostResponse>

    @GET("api/emergency/active")
    @Headers("Accept: application/json")
    suspend fun getActiveEmergencies(
        @Header("Authorization") token: String
    ): Response<EmergencyActiveResponse>

    @GET("api/mytiket")
    @Headers(
        "Accept: application/json"
    )
    suspend fun getMyTiket(
        @Header("Authorization") token: String
    ): Response<MyTiketResponse>

    // ================= SOS / PANIC BUTTON =================

    @POST("api/sos/trigger")
    @Headers("Accept: application/json")
    suspend fun triggerSos(
        @Header("Authorization") token: String,
        @Body request: SosTriggerRequest
    ): Response<SosTriggerResponse>

    @GET("api/sos/active")
    @Headers("Accept: application/json")
    suspend fun getActiveSos(
        @Header("Authorization") token: String
    ): Response<SosActiveResponse>

    @Multipart
    @POST("api/sos/chat/{sos_id}/send")
    @Headers("Accept: application/json")
    suspend fun sendSosChat(
        @Header("Authorization") token: String,
        @Path("sos_id") sosId: Int,
        @Part("type") type: RequestBody,
        @Part("content") content: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Response<SosSendMessageResponse>

    @GET("api/sos/chat/{sos_id}/messages")
    @Headers("Accept: application/json")
    suspend fun getSosMessages(
        @Header("Authorization") token: String,
        @Path("sos_id") sosId: Int,
        @Query("page") page: Int? = null
    ): Response<SosMessagesResponse>

    @GET("api/sos/call-options")
    @Headers("Accept: application/json")
    suspend fun getSosCallOptions(
        @Header("Authorization") token: String
    ): Response<SosCallOptionsResponse>

    @Multipart
    @POST("api/sos/disaster-report")
    @Headers("Accept: application/json")
    suspend fun submitDisasterReport(
        @Header("Authorization") token: String,
        @Part("potensi_bencana") potensiBencana: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part("lokasi") lokasi: RequestBody,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part lampiran: MultipartBody.Part?
    ): Response<DisasterReportResponse>

    @GET("api/sos/disaster-reports")
    @Headers("Accept: application/json")
    suspend fun getMyDisasterReports(
        @Header("Authorization") token: String
    ): Response<DisasterReportsListResponse>

}