package com.arny.mobilebert.utils.strings

import android.content.Context

interface IWrappedString {
    fun toString(context: Context): String?
}