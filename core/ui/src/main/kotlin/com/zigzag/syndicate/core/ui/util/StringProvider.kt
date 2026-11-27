package com.zigzag.syndicate.core.ui.util

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/** Resolves localized strings outside of Compose, e.g. in ViewModels. */
interface StringProvider {
    fun string(@StringRes resId: Int, vararg formatArgs: Any): String
    fun plural(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String
}
