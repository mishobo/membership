package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.dtos.PolicyDTO
import net.lctafrica.membership.api.domain.PlanRepository
import net.lctafrica.membership.api.domain.PlanType
import net.lctafrica.membership.api.domain.Policy
import net.lctafrica.membership.api.domain.PolicyRepository
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service("policyService")
@Transactional
class PolicyService(
    private val repo: PolicyRepository,
    private val planRepo: PlanRepository
) : IPolicyService {

    @Transactional(readOnly = true)
    override fun findAll(): Result<MutableList<Policy>> {
        return ResultFactory.getSuccessResult(repo.findAll())
    }

    @Transactional(readOnly = true)
    override fun findByPlan(planId: Long): Result<MutableList<Policy>> {
        val plan = planRepo.findById(planId)
        return if (plan.isPresent) {
            val policies = repo.findByPlan(plan.get())
            ResultFactory.getSuccessResult(policies)
        } else {
            ResultFactory.getFailResult("No plan with ID $planId was found")
        }
    }

    @Transactional(readOnly = false, rollbackFor = [Exception::class])
    override fun addPolicy(dto: PolicyDTO): Result<Policy> {
        try {
            val plan = planRepo.findById(dto.planId)
            return if (plan.isPresent) {

                // Check that there's no previous policy with overlapping dates with this one
                if (plan.get().type == PlanType.SCHEME) {
                    val oldPol = repo.findByPlan(plan.get()).stream()
                        .filter { p -> p.endDate.isAfter(dto.startDate) }
                        .findFirst()
                    if (oldPol.isPresent)
                        return ResultFactory.getFailResult("Policy overlaps with one starting ${oldPol.get().startDate}")
                }

                val policy = Policy(
                    plan = plan.get(),
                    policyNumber = dto.policyNumber.trim(),
                    startDate = dto.startDate,
                    endDate = dto.endDate
                )
                repo.save(policy)
                ResultFactory.getSuccessResult(policy)
            } else {
                ResultFactory.getFailResult("No plan with ID ${dto.planId} was found")
            }
        } catch (ex: IllegalArgumentException) {
            return ResultFactory.getFailResult(ex.message)
        }
    }

    @Transactional(readOnly = false, rollbackFor = [Exception::class])
    override fun processPolicy(policyNumber: String): Result<Policy> {
        TODO("Not yet implemented")
    }

    override fun findById(policyId: Long): Result<Policy> {
        val policy = repo.findById(policyId).get();
        return ResultFactory.getSuccessResult(policy)
    }
}