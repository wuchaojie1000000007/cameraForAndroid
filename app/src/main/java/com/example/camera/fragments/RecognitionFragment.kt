package com.example.camera.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavArgs
import androidx.navigation.fragment.navArgs
import com.example.camera.R
import com.example.camera.databinding.FragmentRecognitionBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class RecognitionFragment : Fragment() {

    private val args: RecognitionFragmentArgs by navArgs()
    private lateinit var binding: FragmentRecognitionBinding
    private lateinit var textEn: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecognitionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textEn = args.recognition
        binding.textEn.text = textEn

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build()
        val englishChineseTranslator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder().build()

        englishChineseTranslator.downloadModelIfNeeded(conditions)

            .addOnSuccessListener {
                // Model Download success

                englishChineseTranslator.translate(textEn)
                    .addOnSuccessListener { translatedText ->
                        // Translation success
                        binding.textCh.text = translatedText
                    }
                    .addOnFailureListener { exception ->
                        //Translation fail
                        Log.d(TAG, "Translation Failed.")
                    }
            }

            .addOnFailureListener { exception ->
                // Model Download fail
                Log.d(TAG, "Model couldn’t be downloaded or other internal error.")
                Toast.makeText(
                    requireContext(),
                    "Translation Model couldn’t be downloaded or other internal error.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    companion object {
        private val TAG = "RecognitionFragment"
    }

}