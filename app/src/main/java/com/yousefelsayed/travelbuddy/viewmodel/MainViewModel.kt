package com.yousefelsayed.travelbuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yousefelsayed.travelbuddy.model.LanguagesModel
import com.yousefelsayed.travelbuddy.model.ModelsModel
import com.yousefelsayed.travelbuddy.model.TranslationModel
import com.yousefelsayed.travelbuddy.repository.MainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(private val repository: MainRepository): ViewModel() {

    private val _translatedText = MutableStateFlow<Resource<TranslationModel>>(Resource.loading())
    val translateResponse get() = _translatedText
    private val _availableLanguages = MutableStateFlow<Resource<LanguagesModel>>(Resource.loading())
    val availableLanguages get() = _availableLanguages
    private val _availableModels = MutableStateFlow<Resource<ModelsModel>>(Resource.loading())
    val availableModels get() = _availableModels

    fun translateText(){
        viewModelScope.launch {
            repository.translateText().catch { error ->
                _translatedText.value = Resource.error(error.message ?: "Something went wrong")
            }.collect { result ->
                _translatedText.value = Resource.success(result)
            }
        }
    }
    fun loadSupportedModel(){
        viewModelScope.launch {
            repository.getAvailableModels().catch {
            }.collect { result ->
                if (result.models.isNotEmpty()){
                    repository.updateSelectedModel(result.models.first())
                }
            }
        }
    }
    fun getAvailableLanguages(){
        viewModelScope.launch {
            _availableLanguages.value = Resource.loading()
            repository.getAvailableLanguages().catch { error ->
                _availableLanguages.value = Resource.error(error.message ?: "Something went wrong")
            }.collect { result ->
                if(result.lang.isNotEmpty()){
                    if(getOGLang() == "" && getTargetLang() == ""){
                        repository.updateOGLang(newLang = result.lang.first(), newCode = result.langMap[result.lang.first()] ?: "en_XX")
                        repository.updateTargetLang(newLang = result.lang[1], newCode = result.langMap[result.lang[1]] ?: "en_XX")
                    }
                    _availableLanguages.value = Resource.success(result)
                }else {
                    _availableLanguages.value = Resource.error("No languages found")
                }
            }
        }
    }
    fun getAvailableModels(){
        viewModelScope.launch {
            _availableModels.value = Resource.loading()
            repository.getAvailableModels().catch { error ->
                _availableModels.value = Resource.error(error.message ?: "Something went wrong")
            }.collect { result ->
                if (result.models.isNotEmpty()){
                    _availableModels.value = Resource.success(result)
                    repository.updateSelectedModel(result.models.first())
                }else {
                    _availableModels.value = Resource.error("No Models Available")
                }
            }
        }
    }

    private fun getLanguageCode(langName: String): String {
        return _availableLanguages.value.data?.langMap?.get(langName) ?: "en_XX"
    }
    fun getOGText(): String {
        return repository.getOGText()
    }
    fun getOGLang(): String {
        return repository.getOGLang()
    }
    fun getTargetLang(): String {
        return repository.getTargetLang()
    }
    fun getOgLanguageCode(): String {
        //We will use this function for the android speech recognition and the format saved isn't supported
        //so I will return the part needed only
        val stringSplit = repository.getOGLangCode().split("_")
        return stringSplit.first()
    }
    fun getTranslatedLanguageCode(): String {
        //We will use this function for the android speech recognition and the format saved isn't supported
        //so I will return the part needed only
        val stringSplit = repository.getTargetLangCode().split("_")
        return stringSplit.first()
    }
    fun updateOGText(newText: String) {
        viewModelScope.launch {
            repository.updateOGText(newText)
        }
    }
    fun updateOGLang(newLang: String){
        viewModelScope.launch {
            repository.updateOGLang(newLang = newLang, newCode = getLanguageCode(newLang))
        }
    }
    fun updateTargetLang(newLang: String){
        viewModelScope.launch {
            repository.updateTargetLang(newLang = newLang, newCode = getLanguageCode(newLang))
        }
    }
    fun updateSelectedModel(newModel: String) {
        viewModelScope.launch {
            repository.updateSelectedModel(newModel)
        }
    }
}