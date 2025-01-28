package com.arny.mobilebert.di.modules

import android.content.Context
import com.arny.mobilebert.data.ai.analyse.AIModelFactory
import com.arny.mobilebert.data.ai.analyse.BertAnalyzerMultiLang
import com.arny.mobilebert.data.ai.attention.AttentionVisualizer
import com.arny.mobilebert.data.ai.test.ModelComparisonManager
import com.arny.mobilebert.data.ai.test.ModelTestManager
import com.arny.mobilebert.data.search.SmartSearchManager
import com.arny.mobilebert.data.search.TextFileProcessor
import com.arny.mobilebert.data.utils.AndroidAssetManager
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.IModelFileManager
import com.arny.mobilebert.domain.ai.ITestManager
import com.arny.mobilebert.domain.ai.ITextAnalyzer
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
interface DataModule {

    companion object {
        @Provides
        @Singleton
        fun provideAttentionVisualizer(): AttentionVisualizer {
            return AttentionVisualizer()
        }

        @Provides
        @Singleton
        fun provideTextFileProcessor(context: Context): TextFileProcessor {
            return TextFileProcessor(context)
        }

        @Provides
        @Singleton
        fun provideSmartSearchManager(context: Context): SmartSearchManager {
            return SmartSearchManager(context)
        }

        @Provides
        @Singleton
        fun provideAIModelFactory(modelFileManager: ModelFileManager): AIModelFactory {
            return AIModelFactory(modelFileManager)
        }

        @Provides
        @Singleton
        fun provideModelComparisonManager(modelFactory: AIModelFactory): ModelComparisonManager {
            return ModelComparisonManager(modelFactory)
        }


        @Provides
        @Singleton
        fun provideAndroidAssetLoader(context: Context): AndroidAssetManager {
            return AndroidAssetManager(context)
        }
    }

    @Binds
    @Singleton
    fun bindsBertAnalyzerMultiLang(impl: BertAnalyzerMultiLang): ITextAnalyzer

    @Binds
    @Singleton
    fun bindsModelTestManager(impl: ModelTestManager): ITestManager

    @Binds
    @Singleton
    fun bindsModelFileManager(impl: ModelFileManager): IModelFileManager

}