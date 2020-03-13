package com.procurement.revision.infrastructure.web.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.procurement.revision.application.service.Logger
import com.procurement.revision.domain.enums.EnumElementProvider
import com.procurement.revision.domain.functional.Result
import com.procurement.revision.domain.functional.bind
import com.procurement.revision.domain.util.extension.nowDefaultUTC
import com.procurement.revision.domain.util.extension.tryUUID
import com.procurement.revision.infrastructure.configuration.properties.GlobalProperties
import com.procurement.revision.infrastructure.extension.tryGetAttribute
import com.procurement.revision.infrastructure.extension.tryGetAttributeAsEnum
import com.procurement.revision.infrastructure.extension.tryGetTextAttribute
import com.procurement.revision.infrastructure.fail.Fail
import com.procurement.revision.infrastructure.fail.error.BadRequest
import com.procurement.revision.infrastructure.fail.error.DataErrors
import com.procurement.revision.infrastructure.utils.tryToNode
import com.procurement.revision.infrastructure.utils.tryToObject
import java.util.*

enum class CommandType(@JsonValue override val key: String) : Action, EnumElementProvider.Key {

    GET_AMENDMENTS_IDS("getAmendmentIds"),
    DATA_VALIDATION("dataValidation"),
    CREATE_AMENDMENT("createAmendment");

    override fun toString(): String = key

    companion object : EnumElementProvider<CommandType>(info = info()) {
        @JvmStatic
        @JsonCreator
        fun creator(name: String) = CommandType.orThrow(name)
    }
}

fun generateResponseOnFailure(
    fail: Fail,
    version: ApiVersion,
    id: UUID,
    logger: Logger
): ApiResponse {
    fail.logging(logger)
    return when (fail) {
        is Fail.Error -> {
            when (fail) {
                is DataErrors.Validation ->
                    ApiDataErrorResponse(
                        version = version,
                        id = id,
                        result = listOf(
                            ApiDataErrorResponse.Error(
                                code = getFullErrorCode(fail.code),
                                description = fail.description,
                                details = listOf(
                                    ApiDataErrorResponse.Error.Detail(name = fail.name)
                                )
                            )
                        )
                    )
                else -> ApiFailResponse(
                    version = version,
                    id = id,
                    result = listOf(
                        ApiFailResponse.Error(
                            code = getFullErrorCode(fail.code),
                            description = fail.description
                        )
                    )
                )
            }
        }
        is Fail.Incident -> {
            val errors = listOf(
                ApiIncidentResponse.Incident.Details(
                    code = getFullErrorCode(fail.code),
                    description = fail.description,
                    metadata = null
                )
            )
            generateIncident(errors, version, id)
        }
    }
}

private fun generateIncident(
    details: List<ApiIncidentResponse.Incident.Details>, version: ApiVersion, id: UUID
): ApiIncidentResponse =
    ApiIncidentResponse(
        version = version,
        id = id,
        result = ApiIncidentResponse.Incident(
            date = nowDefaultUTC(),
            id = UUID.randomUUID(),
            service = ApiIncidentResponse.Incident.Service(
                id = GlobalProperties.service.id,
                version = GlobalProperties.service.version,
                name = GlobalProperties.service.name
            ),
            details = details
        )
    )

fun getFullErrorCode(code: String): String = "${code}/${GlobalProperties.service.id}"

val NaN: UUID
    get() = UUID(0, 0)

fun JsonNode.tryGetVersion(): Result<ApiVersion, DataErrors> {
    val name = "version"
    return tryGetTextAttribute(name).bind {
        when (val result = ApiVersion.tryValueOf(it)) {
            is Result.Success -> result
            is Result.Failure -> Result.failure(
                DataErrors.Validation.DataFormatMismatch(
                    name = name,
                    expectedFormat = "00.00.00",
                    actualValue = it
                )
            )
        }
    }
}

fun JsonNode.tryGetAction(): Result<CommandType, DataErrors> =
    tryGetAttributeAsEnum("action", CommandType)

fun <T : Any> JsonNode.tryGetParams(target: Class<T>): Result<T, Fail.Error> {
    val name = "params"
    return tryGetAttribute(name).bind {
        when (val result = it.tryToObject(target)) {
            is Result.Success -> result
            is Result.Failure -> Result.failure(
                BadRequest("Error parsing '$name'")
            )
        }
    }
}

fun JsonNode.tryGetId(): Result<UUID, DataErrors> {
    val name = "id"
    return tryGetTextAttribute(name)
        .bind {
            when (val result = it.tryUUID()) {
                is Result.Success -> result
                is Result.Failure -> Result.failure(
                    DataErrors.Validation.DataFormatMismatch(
                        name = name,
                        actualValue = it,
                        expectedFormat = "uuid"
                    )
                )
            }
        }
}

fun String.tryGetNode(): Result<JsonNode, BadRequest> =
    when (val result = this.tryToNode()) {
        is Result.Success -> result
        is Result.Failure -> Result.failure(BadRequest())
    }



