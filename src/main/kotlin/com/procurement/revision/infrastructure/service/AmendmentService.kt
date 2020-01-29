package com.procurement.revision.infrastructure.service

import com.procurement.revision.application.exception.ErrorException
import com.procurement.revision.application.exception.ErrorType
import com.procurement.revision.application.repository.AmendmentRepository
import com.procurement.revision.application.service.amendment.CheckExistingAmendmentForCancelLotContext
import com.procurement.revision.application.service.amendment.CheckExistingAmendmentForCancelLotResult
import com.procurement.revision.application.service.amendment.CheckExistingAmendmentForCancelTenderContext
import com.procurement.revision.application.service.amendment.CheckExistingAmendmentForCancelTenderResult
import com.procurement.revision.application.service.amendment.ProceedAmendmentData
import com.procurement.revision.application.service.amendment.ProceedAmendmentLotCancellationContext
import com.procurement.revision.application.service.amendment.ProceedAmendmentResult
import com.procurement.revision.application.service.amendment.ProceedAmendmentTenderCancellationContext
import com.procurement.revision.domain.enums.AmendmentRelatesTo
import com.procurement.revision.domain.enums.AmendmentStatus
import com.procurement.revision.domain.enums.AmendmentType
import com.procurement.revision.domain.enums.DocumentType
import com.procurement.revision.domain.model.Amendment
import com.procurement.revision.domain.model.Owner
import com.procurement.revision.domain.model.Token
import com.procurement.revision.infrastructure.dto.converter.convert
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AmendmentService(
    private val amendmentRepository: AmendmentRepository,
    private val generationService: GenerationService
) {

    fun proceedAmendmentForTenderCancellation(
        context: ProceedAmendmentTenderCancellationContext,
        data: ProceedAmendmentData
    ): ProceedAmendmentResult {

        val documentTypes = data.amendment.documents
            .associateBy(
                { it.id },
                { it.documentType }
            )
        checkDocumentsTypeForCancellation(documents = documentTypes)  // VR-3.17.1

        val createdAmendment = createAmendment(
            token = context.token,
            owner = context.owner,
            data = data,
            date = context.startDate,
            relatesTo = AmendmentRelatesTo.TENDER,
            relatedItem = context.id
        )

        amendmentRepository.saveNewAmendment(cpid = context.cpid, amendment = createdAmendment)
        return createdAmendment.convert()
    }

    fun proceedAmendmentForLotCancellation(
        context: ProceedAmendmentLotCancellationContext,
        data: ProceedAmendmentData
    ): ProceedAmendmentResult {
        val documentTypes = data.amendment.documents
            .associateBy(
                { it.id },
                { it.documentType }
            )
        checkDocumentsTypeForCancellation(documents = documentTypes)  // VR-3.17.1

        val createdAmendment = createAmendment(
            token = context.token,
            owner = context.owner,
            data = data,
            date = context.startDate,
            relatesTo = AmendmentRelatesTo.LOT,
            relatedItem = context.id.toString()
        )

        amendmentRepository.saveNewAmendment(cpid = context.cpid, amendment = createdAmendment)
        return createdAmendment.convert()
    }

    fun checkExistingAmendmentForCancelLot(
        context: CheckExistingAmendmentForCancelLotContext
    ): CheckExistingAmendmentForCancelLotResult {
        val amendmentsDB = amendmentRepository.findBy(context.cpid)
        val lotId = context.id.toString()
        amendmentsDB.asSequence()
            .filter { amendment ->
                amendment.type == AmendmentType.CANCELLATION && amendment.status == AmendmentStatus.PENDING
            }
            .forEach { amendment ->
                if (amendment.relatesTo == AmendmentRelatesTo.LOT && amendment.relatedItem == lotId)
                    throw ErrorException(
                        error = ErrorType.UNEXPECTED_AMENDMENT,
                        message = """ Found amendment assigned to lot for cancelling. Amendment id=${amendment.id}"""
                    )

                if (amendment.relatesTo == AmendmentRelatesTo.TENDER)
                    throw ErrorException(
                        error = ErrorType.UNEXPECTED_AMENDMENT,
                        message = """ Found amendment assigned to tender for cancelling. Amendment id=${amendment.id}"""
                    )
            }

        return CheckExistingAmendmentForCancelLotResult()
    }

    fun checkExistingAmendmentForCancelTender(
        context: CheckExistingAmendmentForCancelTenderContext
    ): CheckExistingAmendmentForCancelTenderResult {
        val amendmentsDB = amendmentRepository.findBy(context.cpid)
        amendmentsDB.asSequence()
            .filter { amendment ->
                amendment.type == AmendmentType.CANCELLATION && amendment.status == AmendmentStatus.PENDING
            }
            .forEach { amendment ->
                if (amendment.relatesTo == AmendmentRelatesTo.LOT)
                    throw ErrorException(
                        error = ErrorType.UNEXPECTED_AMENDMENT,
                        message = """ Found amendment assigned to lot relates to current tender for cancelling. 
                                     Amendment id=${amendment.id}"""
                    )

                if (amendment.relatesTo == AmendmentRelatesTo.TENDER)
                    throw ErrorException(
                        error = ErrorType.UNEXPECTED_AMENDMENT,
                        message = """ Found amendment assigned to tender for cancelling. Amendment id=${amendment.id}"""
                    )
            }

        return CheckExistingAmendmentForCancelTenderResult()
    }

    private fun checkDocumentsTypeForCancellation(documents: Map<String, DocumentType>) {
        documents.forEach { (id, type) ->
            when (type) {
                DocumentType.CANCELLATION_DETAILS,
                DocumentType.CONFLICT_OF_INTEREST -> Unit

                DocumentType.EVALUATION_CRITERIA,
                DocumentType.ELIGIBILITY_CRITERIA,
                DocumentType.BILL_OF_QUANTITY,
                DocumentType.ILLUSTRATION,
                DocumentType.MARKET_STUDIES,
                DocumentType.TENDER_NOTICE,
                DocumentType.BIDDING_DOCUMENTS,
                DocumentType.PROCUREMENT_PLAN,
                DocumentType.TECHNICAL_SPECIFICATIONS,
                DocumentType.CONTRACT_DRAFT,
                DocumentType.HEARING_NOTICE,
                DocumentType.CLARIFICATIONS,
                DocumentType.ENVIRONMENTAL_IMPACT,
                DocumentType.ASSET_AND_LIABILITY_ASSESSMENT,
                DocumentType.RISK_PROVISIONS,
                DocumentType.COMPLAINTS,
                DocumentType.NEEDS_ASSESSMENT,
                DocumentType.FEASIBILITY_STUDY,
                DocumentType.PROJECT_PLAN,
                DocumentType.SHORTLISTED_FIRMS,
                DocumentType.EVALUATION_REPORTS,
                DocumentType.CONTRACT_ARRANGEMENTS,
                DocumentType.CONTRACT_GUARANTEES  -> throw ErrorException(
                    error = ErrorType.INVALID_DOCUMENT_TYPE,
                    message = "Documents with id=${id} has not allowed type='${type}'."
                )
            }
        }
    }

    private fun createAmendment(
        data: ProceedAmendmentData,
        date: LocalDateTime,
        relatesTo: AmendmentRelatesTo,
        relatedItem: String,
        token: Token,
        owner: Owner
    ): Amendment {
        return Amendment(
            id = generationService.generateAmendmentId(),
            token = token,
            owner = owner,
            rationale = data.amendment.rationale,
            description = data.amendment.description,
            documents = data.amendment.documents
                .map { document ->
                    Amendment.Document(
                        id = document.id,
                        documentType = document.documentType,
                        description = document.description,
                        title = document.title
                    )
                },
            status = AmendmentStatus.PENDING,
            type = AmendmentType.CANCELLATION,
            date = date,
            relatesTo = relatesTo,
            relatedItem = relatedItem
        )
    }
}
