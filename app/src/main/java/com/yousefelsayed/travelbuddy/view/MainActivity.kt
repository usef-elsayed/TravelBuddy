package com.yousefelsayed.travelbuddy.view

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.permissionx.guolindev.PermissionX
import com.yousefelsayed.travelbuddy.R
import com.yousefelsayed.travelbuddy.databinding.ActivityMainBinding
import com.yousefelsayed.travelbuddy.util.NetworkManager
import com.yousefelsayed.travelbuddy.viewmodel.MainViewModel
import com.yousefelsayed.travelbuddy.viewmodel.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var view: ActivityMainBinding
    private val viewModel by viewModel<MainViewModel>()
    private val speechRecognizerTimeout = 5000L
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var networkManager: NetworkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        view = DataBindingUtil.setContentView(this,R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        init()
    }

    private fun init(){
        view.ogTextView.text = viewModel.getOGText()
        networkManager = NetworkManager(this@MainActivity)
        requestPermissions()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        tts = TextToSpeech(this,null)
        setupListeners()
        setupObservers()
        checkForInternet()
    }
    private fun requestPermissions(){
        PermissionX.init(this)
            .permissions(listOf(android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.ACCESS_NETWORK_STATE))
            .request { allGranted, _, deniedList ->
                if (!allGranted) {
                    showSnackBar("These permissions are denied: $deniedList, some functions may not work")
                }
            }
    }
    private fun setupListeners(){
        view.ogLangDropDown.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String
            viewModel.updateOGLang(selectedItem)
        }
        view.translatedLangDropDown.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String
            viewModel.updateTargetLang(selectedItem)
            if(view.ogTextView.tag == "1"){
                viewModel.translateText()
            }
        }
        view.modelSelectionButton.setOnClickListener {
            if (networkManager.checkForInternet()){
                showSnackBar("Retrieving all available models from server")
                view.modelSelectionButton.isClickable = false
                viewModel.getAvailableModels()
            }
        }
        view.recordButton.setOnClickListener {
            if(view.recordButton.tag.toString() == "0") startListening()
            else stopListening()
        }
        view.speakButton.setOnClickListener {
            val result = tts.setLanguage(Locale(viewModel.getTranslatedLanguageCode()))
            if (result == TextToSpeech.SUCCESS){
                tts.speak(view.translatedTextView.text.toString(), TextToSpeech.QUEUE_FLUSH, null,"")
            }else {
                showSnackBar("This language isn't supported")
            }
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                showSnackBar("Please speak now")
                startTimeOut()
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsdB: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
            }

            override fun onError(error: Int) {
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches?.get(0) != null){
                    val finalText = matches[0]
                    viewModel.updateOGText(finalText)
                    view.ogTextView.text = finalText
                    view.ogTextView.tag = "1"
                    viewModel.translateText()
                }else {
                    showSnackBar("No speech recognized")
                }
                stopListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    private fun setupObservers(){
        lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.translateResponse.collect{ result ->
                    when(result.status){
                        Status.LOADING -> {
                        }
                        Status.SUCCESS -> {
                            view.translatedTextView.text = result.data?.translated ?: "Null"
                        }
                        Status.ERROR -> {
                            showSnackBar("Something went wrong while translating ${result.message}")
                        }
                    }
                }
            }
        }
        lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.availableLanguages.collect{ result ->
                    when(result.status){
                        Status.LOADING -> {
                        }
                        Status.SUCCESS -> {
                            setupDropdowns(result.data!!.lang)
                        }
                        Status.ERROR -> {
                            showSnackBar("Something went wrong while getting languages ${result.message}")
                        }
                    }
                }
            }
        }
        lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.availableModels.collect{ result ->
                    when(result.status){
                        Status.LOADING -> {
                        }
                        Status.SUCCESS -> {
                            if(result.data != null && !view.modelSelectionButton.isClickable){
                                showModelsAlertDialog(result.data.models)
                            }
                        }
                        Status.ERROR -> {
                            showSnackBar("Something went wrong while retrieving models from the server")
                        }
                    }
                    view.modelSelectionButton.isClickable = true
                }
            }
        }
    }
    private fun showSnackBar(msg: String){
        Snackbar.make(view.mainLayout,msg,Snackbar.LENGTH_SHORT).show()
    }
    private fun checkForInternet(){
        if (networkManager.checkForInternet()){
            viewModel.loadSupportedModel()
            viewModel.getAvailableLanguages()
        }else {
            Snackbar.make(view.mainLayout, "No Internet connection", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    checkForInternet()
                }.show()
        }
    }
    private fun setupDropdowns(languages: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languages)
        view.ogLangDropDown.setAdapter(adapter)
        view.translatedLangDropDown.setAdapter(adapter)

        // Restore or get Default values
        view.ogLangDropDown.setText(viewModel.getOGLang(), false)
        view.translatedLangDropDown.setText(viewModel.getTargetLang(), false)

        // Enable interactivity
        view.ogLangDropDown.isFocusableInTouchMode = true
        view.ogLangDropDown.isFocusable = true
        view.ogLangDropDown.isClickable = true
        view.translatedLangDropDown.isFocusableInTouchMode = true
        view.translatedLangDropDown.isFocusable = true
        view.translatedLangDropDown.isClickable = true
        view.recordButton.isEnabled = true
        view.speakButton.isEnabled = true
        view.modelSelectionButton.isClickable = true
    }
    private fun showModelsAlertDialog(models: List<String>){
        // Create an AlertDialog
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Select a Model")
        builder.setItems(models.toTypedArray()) { _, which ->
            val selectedModel = models[which]
            viewModel.updateSelectedModel(selectedModel)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }
    //STT Control
    private fun startListening() {
        //Update the UI
        view.recordButton.text = "Listening..."
        view.recordButton.tag = "1"
        //Create the Intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, viewModel.getOgLanguageCode())
        }
        //Start Listening
        speechRecognizer.startListening(intent)
    }
    private fun startTimeOut(){
        CoroutineScope(Dispatchers.Main).launch {
            delay(speechRecognizerTimeout)
            stopListening()
        }
    }
    private fun stopListening() {
        //Update the UI
        view.recordButton.text = "Listen"
        view.recordButton.tag = "0"
        //Stop Listening
        speechRecognizer.stopListening()
    }
    override fun onDestroy() {
        speechRecognizer.destroy()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}