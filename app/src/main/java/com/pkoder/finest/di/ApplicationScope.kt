package com.pkoder.finest.di

import javax.inject.Qualifier

/** Qualifies the process-lifetime [kotlinx.coroutines.CoroutineScope] provided by [AppModule]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
