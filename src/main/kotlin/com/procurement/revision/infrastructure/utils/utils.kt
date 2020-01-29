package com.procurement.revision.infrastructure.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.procurement.revision.infrastructure.bind.databinding.JsonDateTimeFormatter
import com.procurement.revision.infrastructure.bind.jackson.configuration
import java.io.IOException
import java.time.LocalDateTime

private object JsonMapper {
    val mapper: ObjectMapper = ObjectMapper().apply {
        configuration()
    }
}

/*Date utils*/
fun String.toLocal(): LocalDateTime {
    return LocalDateTime.parse(this, JsonDateTimeFormatter.formatter)
}

/**
Json utils
 **/
fun <T : Any> T.toJson(): String = try {
    JsonMapper.mapper.writeValueAsString(this)
} catch (expected: JsonProcessingException) {
    val className = this::class.java.canonicalName
    throw IllegalArgumentException("Error mapping an object of type '$className' to JSON.", expected)
}

fun <T : Any> String.toObject(target: Class<T>): T = try {
    JsonMapper.mapper.readValue(this, target)
} catch (expected: Exception) {
    throw IllegalArgumentException("Error binding JSON to an object of type '${target.canonicalName}'.", expected)
}

fun <T : Any> JsonNode.toObject(target: Class<T>): T {
    try {
        return JsonMapper.mapper.treeToValue(this, target)
    } catch (expected: IOException) {
        throw IllegalArgumentException("Error binding JSON to an object of type '${target.canonicalName}'.", expected)
    }
}
