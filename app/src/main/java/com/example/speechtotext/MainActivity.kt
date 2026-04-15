package com.example.speechtotext

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.speechtotext.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var speechIntent: Intent
    private var isListening = false

    // Cấu hình API Langbly
    private val BASE_URL = "https://api.langbly.com/"
    private val API_KEY = "YOUR_API_KEY_HERE" // Thay bằng key của bạn

    private val apiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    data class Language(val name: String, val code: String) {
        override fun toString(): String = name
    }

    private val languages = listOf(
        Language("English", "en"),
        Language("Vietnamese", "vi"),
        Language("French", "fr"),
        Language("Japanese", "ja"),
        Language("Korean", "ko")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupSpeechRecognizer()

        binding.btnRecord.setOnClickListener {
            if (isListening) stopListening() else checkPermissionAndStart()
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            binding.tvOriginalText.text = "Máy không hỗ trợ nhận diện giọng nói"
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                binding.tvOriginalText.text = "Đang lắng nghe..."
            }

            override fun onError(error: Int) {
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    stopListening()
                }
                Log.e("Speech", "Error code: $error")
                isListening = false
                binding.btnRecord.text = "Bắt đầu"
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    binding.tvOriginalText.text = text
                    translateText(text)
                }
                isListening = false
                binding.btnRecord.text = "Bắt đầu"
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) binding.tvOriginalText.text = text
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun translateText(text: String) {
        val selectedLang = binding.spinnerLanguage.selectedItem as Language
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val sourceLang = Locale.getDefault().language

                val request = TranslationRequest(
                    q = text,
                    source = sourceLang,
                    target = selectedLang.code
                )

                val response = withContext(Dispatchers.IO) {
                    apiService.translate("Bearer $API_KEY", request)
                }

                if (response.isSuccessful) {
                    val rawBody = response.body()?.string()
                    val translatedText = parseJson(rawBody)
                    binding.tvTranslatedText.text = translatedText
                } else {
                    val error = response.errorBody()?.string()
                    binding.tvTranslatedText.text = "Lỗi dịch: Kiểm tra API Key"
                    Log.e("API", "Error: $error")
                }
            } catch (e: Exception) {
                binding.tvTranslatedText.text = "Lỗi kết nối: ${e.message}"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun parseJson(jsonStr: String?): String {
        if (jsonStr == null) return ""
        return try {
            val json = JSONObject(jsonStr)
            val nested = json.optJSONObject("data")
                ?.optJSONArray("translations")
                ?.optJSONObject(0)
                ?.optString("translatedText", "")
            if (!nested.isNullOrBlank()) return nested
            
            json.optString("translatedText", "")
        } catch (e: Exception) { "" }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        isListening = true
        binding.btnRecord.text = "Dừng"
        speechRecognizer?.startListening(speechIntent)
    }

    private fun stopListening() {
        isListening = false
        binding.btnRecord.text = "Bắt đầu"
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
