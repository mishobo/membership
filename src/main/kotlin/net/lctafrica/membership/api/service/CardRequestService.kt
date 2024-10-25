package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.CardBatch
import net.lctafrica.membership.api.domain.CardBatchRepository
import net.lctafrica.membership.api.domain.CardStatus
import net.lctafrica.membership.api.domain.PolicyRepository
import net.lctafrica.membership.api.dtos.CardDTO
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import javax.validation.Valid

@Service
@Transactional
class CardRequestService(val repo: CardBatchRepository, val policyRepo: PolicyRepository) : ICardRequestService {

    @Transactional(rollbackFor = [Exception::class])
    override fun newRequest(@Valid dto: CardDTO): Result<CardBatch> {
        return try {
            val policy = policyRepo.findById(dto.policyId)
            if (policy.isEmpty) return ResultFactory.getFailResult("No policy with ID ${dto.policyId}")
            policy.map {
                CardBatch(
                    policy = it, type = dto.type,
                    createdAt = LocalDate.now()
                )
            }.map {
                repo.save(it)
            }.map { ResultFactory.getSuccessResult(it) }.get()

        } catch (ex: IllegalArgumentException) {
            ResultFactory.getFailResult(msg = ex.message)
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun issueCardBatch(batchId: Long): Result<CardBatch> {
        val findById = repo.findById(batchId)
        return findById.map {
            it.modifiedAt = LocalDateTime.now()
            it.status = CardStatus.ISSUED
            repo.save(it)
            ResultFactory.getSuccessResult(it)
        }.orElseGet {
            ResultFactory.getFailResult("No such batch exists")
        }
    }

    @Transactional(readOnly = true)
    override fun findByMember(memberNumber: String): Result<List<CardBatch>> {
        val list = repo.searchByMemberNumber(memberNumber)
        return ResultFactory.getSuccessResult(list)
    }
}