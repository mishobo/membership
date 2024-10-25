package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.dtos.PlanDTO
import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service("planService")
@Transactional
class PlanService(
    private val repo: PlanRepository,
    private val payerRepo: PayerRepository,
    private val categoryRepository: CategoryRepository,
) : IPlanService {

    @Transactional(readOnly = true)
    override fun findAll(page: Int, size: Int): Result<Page<Plan>> {
        val request = PageRequest.of(page - 1, size)
        val res = repo.findAll(request)
        return ResultFactory.getSuccessResult(res)
    }

    /*@Transactional(readOnly = true)
    override fun findByPayer(payerId: Long): Result<MutableList<Plan>> {
        val existingPayer = payerRepo.findById(payerId)
        return if (existingPayer.isPresent) {
            ResultFactory.getSuccessResult(repo.findByPayer(existingPayer.get()))
        } else {
            ResultFactory.getFailResult("No payer with ID $payerId was found")
        }
    }*/

    @Transactional(readOnly = false, rollbackFor = [Exception::class])
    override fun addPlan(dto: PlanDTO): Result<Plan> {
        /*val existingPayer = payerRepo.findById(dto.payerId)
        if (existingPayer.isPresent) {*/
        val existingPlan = repo.findByNameIgnoreCase(dto.name.trim())
        if (existingPlan.isPresent) {
            return ResultFactory.getFailResult("Plan [${dto.name}] already exists")
        }
        val plan = Plan(
            name = dto.name.trim(),
            type = dto.type,
            //payer = existingPayer.get(),
            policies = mutableListOf(),
            accessMode = dto.benefitAccessMode
        )
        return ResultFactory.getSuccessResult(repo.save(plan))
        /*} else {
            return ResultFactory.getFailResult("No payer with ID ${dto.payerId} was found")
        }*/
    }

    override fun findByCategory(categoryId: Long): Result<Plan> {
        val category: Optional<Category> = categoryRepository.findById(categoryId)
        if(category.isPresent){

            return ResultFactory.getSuccessResult(data = category.get().policy.plan)
        }
        return ResultFactory.getFailResult(msg = "No category $categoryId was found ")

    }

    override fun findByPayerId(payerId: Long): Result<MutableList<Plan>> {
        val payerPlans = repo.findByPayer(payerId)
        return if(payerPlans.isNotEmpty()){
            ResultFactory.getSuccessResult(data = payerPlans)
        }else {
            ResultFactory.getFailResult(msg = "No plan(s) found for payer id  $payerId")
        }

    }
}
