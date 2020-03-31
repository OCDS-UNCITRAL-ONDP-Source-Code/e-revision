package com.procurement.revision.infrastructure.fail.error

import com.procurement.revision.domain.model.amendment.AmendmentId
import com.procurement.revision.domain.model.document.DocumentId
import com.procurement.revision.infrastructure.fail.Fail

sealed class ValidationError(
    numberError: String,
    override val description: String,
    val id: String? = null
) : Fail.Error("VR-") {
    override val code: String = prefix + numberError

    class InvalidDocumentType(documentId: DocumentId) : ValidationError(
        numberError = "1",
        description = "Document '${documentId}' has invalid documentType.",
        id = documentId
    )

    class InvalidToken() : ValidationError(
        numberError = "10.2.4.1",
        description = "Request token doesn't match token from the database."
    )

    class InvalidOwner() : ValidationError(
        numberError = "10.2.4.2",
        description = "Request owner doesn't match owner from the database."
    )

    class AmendmentNotFound(id: AmendmentId) : ValidationError(
        numberError = "10.2.4.3",
        description = "Amendment not found.",
        id = id.toString()
    )
}