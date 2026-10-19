package com.mobile.payment.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** A fresh key for one payment attempt (letters, digits and '-', as the backend expects). */
@OptIn(ExperimentalUuidApi::class)
fun newIdempotencyKey(): String = Uuid.random().toString()
