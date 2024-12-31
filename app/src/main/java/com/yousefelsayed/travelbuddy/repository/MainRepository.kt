package com.yousefelsayed.travelbuddy.repository

import android.util.Log
import com.yousefelsayed.travelbuddy.model.LanguagesModel
import com.yousefelsayed.travelbuddy.model.ModelsModel
import com.yousefelsayed.travelbuddy.model.SessionModel
import com.yousefelsayed.travelbuddy.model.TranslationModel
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MainRepository(private val httpClient: HttpClient) {

    private val session = SessionModel()
    private val baseURL = "REPLACE-WITH-YOUR-API-URL"

    suspend fun translateText(): Flow<TranslationModel>{
        return flow {
            emit(httpClient.get<TranslationModel>("$baseURL/translate"){
                Log.d("Debug","Sent a request to server with this model: ${session.selectedModel}")
                url {
                    parameters.append("model",session.selectedModel)
                    parameters.append("text",session.ogText)
                    parameters.append("sourceLang",session.ogLangCode)
                    parameters.append("targetLang",session.targetLangCode)
                }
            })
        }
    }
    suspend fun getAvailableLanguages(): Flow<LanguagesModel>{
        return flow {
            emit(httpClient.get<LanguagesModel>("$baseURL/languages"))
        }
    }
    suspend fun getAvailableModels(): Flow<ModelsModel>{
        return flow {
            emit(httpClient.get<ModelsModel>("$baseURL/models"))
        }
    }

    fun getOGText(): String{
        return session.ogText
    }
    fun getOGLang(): String {
        return session.ogLang
    }
    fun getTargetLang(): String {
        return session.targetLang
    }
    fun getOGLangCode(): String {
        return session.ogLangCode
    }
    fun getTargetLangCode(): String {
        return session.targetLangCode
    }
    suspend fun updateOGText(newText: String) {
        session.ogText = newText
    }
    suspend fun updateSelectedModel(newModel: String){
        session.selectedModel = newModel
    }
    suspend fun updateOGLang(newLang: String, newCode: String){
        session.ogLang = newLang
        session.ogLangCode = newCode
    }
    suspend fun updateTargetLang(newLang: String, newCode: String){
        session.targetLang = newLang
        session.targetLangCode = newCode
    }
}