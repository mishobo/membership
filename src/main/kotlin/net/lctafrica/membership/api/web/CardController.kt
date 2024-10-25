package net.lctafrica.membership.api.web

import net.lctafrica.membership.api.dtos.CardDTO
import net.lctafrica.membership.api.service.ICardRequestService
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/v1/card")
class CardController(val service: ICardRequestService) : AbstractController() {

    @PostMapping(value = [""], produces = ["application/json"])
    fun newRequest(@Valid @RequestBody dto: CardDTO) = service.newRequest(dto)

    @PutMapping(value = ["/{batchId}/issue"], produces = ["application/json"])
    fun issueBatch(@PathVariable(value = "batchId") batchId: Long) = service.issueCardBatch(batchId)

    @GetMapping(value = [""])
    fun findByMember(@RequestParam(value = "memberNumber") memberNumber: String) = service.findByMember(memberNumber)
}