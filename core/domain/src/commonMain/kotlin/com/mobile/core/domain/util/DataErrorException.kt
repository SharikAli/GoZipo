package com.mobile.core.domain.util

class DataErrorException(
    val error: DataError
): Exception()