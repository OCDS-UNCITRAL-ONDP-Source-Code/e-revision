package com.procurement.revision.infrastructure.converter

import com.procurement.revision.application.exception.ErrorException
import com.procurement.revision.application.exception.ErrorType
import com.procurement.revision.application.model.amendment.GetAmendmentIdsData
import com.procurement.revision.infrastructure.web.dto.request.amendment.GetAmendmentIdsRequest
import com.procurement.revision.lib.errorIfEmpty

fun GetAmendmentIdsRequest.convert(): GetAmendmentIdsData {
    return GetAmendmentIdsData(
        status = status,
        type = type,
        relatesTo = relatesTo,
        relatedItems = relatedItems.errorIfEmpty {
            ErrorException(
                error = ErrorType.IS_EMPTY,
                message = "The amendment with cpid '${cpid}' contains empty list of relatedItems."
            )
        }
            ?.toList(),
        cpid = cpid,
        ocid = ocid
    )
}
