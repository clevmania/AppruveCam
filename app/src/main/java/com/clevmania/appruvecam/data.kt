package com.clevmania.appruvecam

import okhttp3.RequestBody
import retrofit2.http.*

/**
 * @author by Lawrence on 5/9/20.
 * for AppruveCam
 */
interface UploadDocumentService {
    //https://stage.appruve.co/v1/verifications/test/file_upload
    @PUT("v1/verifications/test/file_upload")
    suspend fun uploadDocument(
        @Body requestBody: RequestBody
    ): String
}

interface DocumentDataSource {
    suspend fun uploadDocument(img: RequestBody): String
}

class DocumentRepository (private val apiService: UploadDocumentService) : DocumentDataSource {
    override suspend fun uploadDocument(img: RequestBody): String {
        return apiService.uploadDocument(requestBody = img)
    }

}