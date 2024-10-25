package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.dtos.CopayDTO
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service("copaySetupService")
@Transactional
class CopaySetupService(
    private val repo: CopaySetupRepository,
    private val providerRepo: ProviderRepository,
    private val categoryRepo: CategoryRepository
) :
    ICopaySetupService {

    @Transactional(rollbackFor = [Exception::class])
    override fun addCopay(dto: CopayDTO): Result<Copayment> {
        val category = categoryRepo.findById(dto.categoryId)
        val provider = providerRepo.findById(dto.providerId)

        if (category.isEmpty) return ResultFactory.getFailResult("No category with ID ${dto.categoryId} was found")
        if (provider.isEmpty) return ResultFactory.getFailResult("No provider with ID ${dto.providerId} was found")

        val optional = repo.findByCategoryAndProvider(category.get(), provider.get())
        if (optional.isPresent) {
            return ResultFactory.getFailResult(
                data = optional.get(),
                msg = "This copayment for ${provider.get().name} has already been set up"
            )
        }
        var copay = Copayment(
            provider = provider.get(),
            category = category.get(),
            amount = dto.amount,
            id = null,
            status = Status.ACTIVE,
            processed = false,
            processedTime = null
        )
        repo.save(copay)
        return ResultFactory.getSuccessResult(copay)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun deactivate(copayment: Copayment): Result<Copayment> {
        var copay = repo.findById(copayment.id!!)
        copay.ifPresent {
            it.apply {
                processed = false
                processedTime = null
                status = Status.INACTIVE
            }
            repo.save(it)
        }
        return ResultFactory.getSuccessResult(copay.get())
    }

    @Transactional(readOnly = true)
    override fun findByProvider(providerId: Long): Result<MutableList<Copayment>> {
        val provider = providerRepo.findById(providerId)
        if (provider.isPresent){
            val results = repo.findByProvider(provider.get())
            return ResultFactory.getSuccessResult(results)
        }
        return ResultFactory.getFailResult("No provider with ID $providerId was found")
    }

    @Transactional(readOnly = true)
    override fun findByCategory(categoryId: Long): Result<MutableList<Copayment>> {
        val category = categoryRepo.findById(categoryId)
        if (category.isPresent) {
            val results = repo.findByCategory(category.get())
            return ResultFactory.getSuccessResult(results)
        }

        return ResultFactory.getFailResult( msg = "No category with ID $categoryId was found")
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun process(category: Category): Result<MutableList<Copayment>> {

        val candidates = repo.findByCategory(category).stream().filter { !it.processed }.map {
            it.processed = true
            it.processedTime = LocalDateTime.now()
            return@map it
        }.collect(Collectors.toList())
        //TODO push to benefit management

        repo.saveAll(candidates)

        return ResultFactory.getSuccessResult("Successfully processed copay setups for category ${category.name}")
    }

    @Transactional(readOnly = true)
    override fun findByCategoryAndProvider(categoryId: Long, providerId: Long): Result<Copayment?> {
        val category = categoryRepo.findById(categoryId)
        val provider = providerRepo.findById(providerId)
        if (category.isEmpty || provider.isEmpty){
            return ResultFactory.getFailResult("No such category[$categoryId] or provider[$providerId] was found")
        }
        val copay = repo.findByCategoryAndProvider(category = category.get(), provider.get())
        if(copay.isPresent){
            return ResultFactory.getSuccessResult(data = copay.get())
        }
        return ResultFactory.getFailResult("No copayment was found")

    }
}
