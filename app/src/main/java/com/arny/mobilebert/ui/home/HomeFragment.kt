package com.arny.mobilebert.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.arny.mobilebert.R
import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.ComparisonProgress
import com.arny.mobilebert.data.utils.formatFileSize
import com.arny.mobilebert.databinding.FragmentHomeBinding
import com.arny.mobilebert.utils.launchWhenCreated
import com.arny.mobilebert.utils.viewModelFactory
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.AndroidSupportInjection
import dagger.assisted.AssistedFactory
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

class HomeFragment : Fragment() {

    @AssistedFactory
    internal interface ViewModelFactory {
        fun create(): HomeViewModel
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: HomeViewModel by viewModelFactory { viewModelFactory.create() }

    private lateinit var binding: FragmentHomeBinding

    companion object {
        fun newInstance() = HomeFragment()
    }

    // Регистрируем контракт для выбора файла модели
    private val selectModelFile = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importModelFile(it) }
    }

    // Регистрируем контракт для выбора файла словаря
    private val selectVocabFile = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importVocabFile(it) }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeData()
        setListeners()
    }

    private fun setListeners() {
        // Список моделей для отображения в Spinner
        val values = ModelConfig.values().toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, values)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.modelSelectionSpinner.adapter = adapter
        binding.btnAnalyse.setOnClickListener {
            viewModel.analyzeText(
                binding.inputEditText.text.toString()
            )
        }
        binding.btnSearch.setOnClickListener {
            viewModel.performSearch(
                binding.inputEditText.text.toString()
            )
        }
        binding.btnTest.setOnClickListener {
            viewModel.testModel(
                values[binding.modelSelectionSpinner.selectedItemPosition]
            )
        }
        binding.btnClear.setOnClickListener {
            binding.inputEditText.text.clear()
            binding.resultsTextView.text = ""
        }
        binding.btnLoadModel.setOnClickListener {
            viewModel.startImport(values[binding.modelSelectionSpinner.selectedItemPosition])
            startModelImport()
        }
        binding.btnLoadVocab.setOnClickListener {
            viewModel.startImport(values[binding.modelSelectionSpinner.selectedItemPosition])
            startVocabImport()
        }
        binding.btnCompare.setOnClickListener {
            viewModel.startComparison()
        }
        binding.btnCopyToClipboard.setOnClickListener {
            copyToClipboard()
        }
    }

    private fun startModelImport() {
        selectModelFile.launch("application/octet-stream")
    }

    private fun startVocabImport() {
        selectVocabFile.launch("text/plain")
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    // Расширенная версия копирования в буфер с определением типа
    private fun copyToClipboard() {
        val text = binding.resultsTextView.text.toString()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text/plain", text)
        clipboard.setPrimaryClip(clip)
        showSnackbar("Скопировано в буфер")
    }

    private fun observeData() {
        launchWhenCreated {
            viewModel.resultText.collectLatest { text ->
                binding.resultsTextView.text = text
            }
        }
        launchWhenCreated {
            viewModel.resultText.collectLatest { text ->
                binding.resultsTextView.text = text
            }
        }
        launchWhenCreated {
            viewModel.state.collectLatest { state ->
                val progress = state == HomeUIState.Loading || state is HomeUIState.Progress
                binding.btnTest.isEnabled = !progress
                binding.btnClear.isEnabled = !progress
                binding.btnSearch.isEnabled = !progress
                binding.btnAnalyse.isEnabled = !progress
                when (state) {
                    is HomeUIState.Error -> {
                        binding.resultsTextView.text = state.message
                    }

                    HomeUIState.Idle -> {}

                    HomeUIState.Loading -> {
                        binding.resultsTextView.text = getString(R.string.loading)
                    }

                    is HomeUIState.Progress -> {
                        when (val comparisonProgress = state.progress) {
                            is ComparisonProgress.ModelCompleted -> {
                                binding.resultsTextView.text = buildString {
                                    append("Progress: ${comparisonProgress.modelName} ")
                                    append("Метрики: ${comparisonProgress.metrics}")
                                }
                            }

                            is ComparisonProgress.ModelStarted -> {
                                binding.resultsTextView.text = buildString {
                                    append("ModelStarted ${comparisonProgress.modelName} ")
                                    append("current: ${comparisonProgress.current}")
                                    append("total: ${comparisonProgress.total}")
                                }
                            }

                            is ComparisonProgress.TestProgress -> {
                                binding.resultsTextView.text = buildString {
                                    append("TestProgress ${comparisonProgress.modelName} ")
                                    append("currentTest: ${comparisonProgress.currentTest}")
                                    append("phase: ${comparisonProgress.phase}")
                                    append("progress: ${comparisonProgress.progress}")
                                }
                            }
                        }

                    }

                    is HomeUIState.Results -> {
                        binding.resultsTextView.text = buildString {
                            append("Results ${state.text} ")
                        }
                    }

                    is HomeUIState.Importing -> {
                        binding.resultsTextView.text = buildString {
                            append("Importing ${state.modelConfig.modelPath} ")
                        }
                    }

                    is HomeUIState.Imported -> {
                        binding.resultsTextView.text = buildString {
                            append("Model file: ${state.modelInfo?.modelSize?.let { formatFileSize(it) }}")
                            append(" Vocab file: ${state.modelInfo?.let { formatFileSize(it.vocabSize) }}")
                            append(
                                " Total size: ${
                                    formatFileSize(
                                        (state.modelInfo?.modelSize ?: 0) + (state.modelInfo
                                            ?.vocabSize ?: 0)
                                    )
                                }"
                            )
                            append(" Output shape: ${state.modelInfo?.outputShape?.joinToString()}")
                        }
                    }

                    HomeUIState.SelectingModel -> {
                        binding.resultsTextView.text = buildString {
                            append("SelectingModel")
                        }
                    }

                    HomeUIState.SelectingVocab -> {
                        binding.resultsTextView.text = buildString {
                            append("SelectingVocab")
                        }
                    }
                }

            }
        }
        launchWhenCreated {
            viewModel.uiEnabled.collectLatest { enabled ->
                binding.btnTest.isEnabled = enabled
                binding.btnClear.isEnabled = enabled
                binding.btnSearch.isEnabled = enabled
                binding.btnAnalyse.isEnabled = enabled
            }
        }
    }
}