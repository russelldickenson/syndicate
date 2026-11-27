package com.zigzag.syndicate.core.ui.di

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.zigzag.syndicate.core.ui.util.StringProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidStringProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : StringProvider {
    override fun string(@StringRes resId: Int, vararg formatArgs: Any): String =
        context.getString(resId, *formatArgs)

    override fun plural(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String =
        context.resources.getQuantityString(resId, quantity, *formatArgs)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UiModule {
    @Binds
    @Singleton
    abstract fun bindStringProvider(implementation: AndroidStringProvider): StringProvider
}
