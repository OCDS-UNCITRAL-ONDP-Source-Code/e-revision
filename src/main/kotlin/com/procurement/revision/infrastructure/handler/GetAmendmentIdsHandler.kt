package com.procurement.revision.infrastructure.handler

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.revision.application.service.AmendmentService
import com.procurement.revision.domain.functional.Result
import com.procurement.revision.domain.functional.Result.Companion.failure
import com.procurement.revision.domain.model.amendment.AmendmentId
import com.procurement.revision.infrastructure.converter.convert
import com.procurement.revision.infrastructure.fail.Fail
import com.procurement.revision.infrastructure.web.dto.CommandType
import com.procurement.revision.infrastructure.web.dto.request.amendment.GetAmendmentIdsRequest
import com.procurement.revision.infrastructure.web.dto.tryGetParams
import org.springframework.stereotype.Component

@Component
class GetAmendmentIdsHandler(private val amendmentService: AmendmentService) : AbstractHandler<CommandType, List<AmendmentId>>() {

    override val action: CommandType = CommandType.GET_AMENDMENTS_IDS

    override fun execute(node: JsonNode): Result<List<AmendmentId>, Fail> {
        val params = node
            .tryGetParams(GetAmendmentIdsRequest::class.java)
            .doOnError { error -> return failure(error) }
            .get
            .convert()
            .doOnError { error -> return failure(error) }
            .get

        return amendmentService.getAmendmentIdsBy(params)
    }
}