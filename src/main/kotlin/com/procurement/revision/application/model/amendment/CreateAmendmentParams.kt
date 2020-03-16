package com.procurement.revision.application.model.amendment

import com.procurement.revision.domain.enums.DocumentType
import com.procurement.revision.domain.functional.Option
import com.procurement.revision.domain.functional.Result
import com.procurement.revision.domain.functional.Result.Companion.failure
import com.procurement.revision.domain.functional.Result.Companion.success
import com.procurement.revision.domain.model.Cpid
import com.procurement.revision.domain.model.Ocid
import com.procurement.revision.domain.model.Owner
import com.procurement.revision.domain.model.amendment.AmendmentId
import com.procurement.revision.domain.model.amendment.tryAmendmentId
import com.procurement.revision.domain.model.date.tryParse
import com.procurement.revision.domain.model.document.DocumentId
import com.procurement.revision.domain.model.document.tryDocumentId
import com.procurement.revision.domain.model.tryOwner
import com.procurement.revision.infrastructure.fail.error.DataErrors
import com.procurement.revision.infrastructure.model.OperationType
import java.time.LocalDateTime

class CreateAmendmentParams private constructor(
    val amendment: Amendment,
    val id: String,
    val operationType: OperationType,
    val startDate: LocalDateTime,
    val cpid: Cpid,
    val ocid: Ocid,
    val owner: Owner
) {
    companion object {
        fun tryCreate(
            amendment: Amendment,
            id: String,
            operationType: String,
            startDate: String,
            cpid: String,
            ocid: String,
            owner: String
        ): Result<CreateAmendmentParams, DataErrors> {

            val operationTypeParsed = OperationType.orNull(operationType)
                ?: return failure(
                    DataErrors.Validation.UnknownValue(
                        name = "operationType",
                        actualValue = operationType,
                        expectedValues = OperationType.allowedValues
                    )
                )

            val startDateParsed = startDate.tryParse()
                .doOnError { expectedFormat ->
                    return failure(
                        DataErrors.Validation.DataFormatMismatch(
                            name = "startDate",
                            actualValue = startDate,
                            expectedFormat = expectedFormat
                        )
                    )
                }
                .get

            val ownerParsed = owner.tryOwner()
                .doOnError {
                    return failure(
                        DataErrors.Validation.DataFormatMismatch(
                            name = "owner",
                            actualValue = owner,
                            expectedFormat = "string"
                        )
                    )
                }
                .get

            val cpidParsed = parseCpid(cpid)
                .doReturn { error -> return failure(error = error) }

            val ocidParsed = parseOcid(ocid)
                .doReturn { error -> return failure(error = error) }

            return success(
                CreateAmendmentParams(
                    cpid = cpidParsed,
                    ocid = ocidParsed,
                    operationType = operationTypeParsed,
                    id = id,
                    owner = ownerParsed,
                    amendment = amendment,
                    startDate = startDateParsed
                )
            )
        }
    }

    class Amendment private constructor(
        val rationale: String,
        val description: String?,
        val documents: List<Document>,
        val id: AmendmentId
    ) {
        companion object {
            fun tryCreate(
                id: String,
                rationale: String,
                description: String?,
                documents: Option<List<Document>>
            ): Result<Amendment, DataErrors> {
                if (documents.isDefined && documents.get.isEmpty())
                    return failure(DataErrors.Validation.EmptyArray("amendment.documents"))

                val idParsed = id.tryAmendmentId()
                    .doOnError {
                        return failure(
                            DataErrors.Validation.DataFormatMismatch(
                                name = "amendment.id",
                                expectedFormat = "UUID",
                                actualValue = id
                            )
                        )
                    }
                    .get

                return success(
                    Amendment(
                        id = idParsed,
                        rationale = rationale,
                        description = description,
                        documents = if (documents.isDefined) documents.get else emptyList()
                    )
                )
            }
        }

        override fun equals(other: Any?): Boolean = if (this === other)
            true
        else
            other is Amendment
                && this.id == other.id

        override fun hashCode(): Int = id.hashCode()

        class Document private constructor(
            val documentType: DocumentType,
            val id: DocumentId,
            val title: String,
            val description: String?
        ) {
            companion object {
                fun tryCreate(
                    documentType: String,
                    id: String,
                    title: String,
                    description: String?
                ): Result<Document, DataErrors> {

                    val idParsed = id.tryDocumentId()
                        .doOnError {
                            return failure(
                                DataErrors.Validation.DataFormatMismatch(
                                    name = "document.id",
                                    actualValue = id,
                                    expectedFormat = "string"
                                )
                            )
                        }
                        .get

                    val documentTypeParsed = DocumentType.orNull(documentType) ?: return failure(
                        DataErrors.Validation.UnknownValue(
                            name = "documentType",
                            actualValue = documentType,
                            expectedValues = DocumentType.allowedValues
                        )
                    )

                    return success(
                        Document(
                            id = idParsed,
                            description = description,
                            documentType = documentTypeParsed,
                            title = title
                        )
                    )
                }
            }

            override fun equals(other: Any?): Boolean = if (this === other)
                true
            else
                other is Document
                    && this.id == other.id

            override fun hashCode(): Int = id.hashCode()
        }
    }
}