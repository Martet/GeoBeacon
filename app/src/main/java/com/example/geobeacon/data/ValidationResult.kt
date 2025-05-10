package com.example.geobeacon.data

data class ValidationResult(
    val valid: Boolean,
    val errorStringResource: Int? = null,
    val warning: Boolean = false,
    val additionalString: String = "",
)
