package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.dtos.ExclusionDTO
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service("providerExclusionService")
@Transactional
class ProviderExclusionService(
    private val repo: ProviderExclusionRepository,
    private val providerRepo: ProviderRepository,
    private val categoryRepo: CategoryRepository
) : IProviderExclusionService {

    @Transactional(rollbackFor = [Exception::class])
    override fun addExclusion(dto: ExclusionDTO): Result<ProviderExclusion> {
        val category = categoryRepo.findById(dto.categoryId)
        val provider = providerRepo.findById(dto.providerId)

        if (category.isEmpty) return ResultFactory.getFailResult("No category with ID ${dto.categoryId} was found")
        if (provider.isEmpty) return ResultFactory.getFailResult("No provider with ID ${dto.providerId} was found")

        val optional = repo.findByCategoryAndProvider(category.get(), provider.get())

        if (optional.isPresent) {
            return ResultFactory.getFailResult(
                data = optional.get(),
                msg = "This exclusion for ${provider.get().name} has already been set up"
            )
        }

        var exclusion =
            ProviderExclusion(
                id = null,
                provider = provider.get(),
                category = category.get(),
                status = Status.ACTIVE
            )
        repo.save(exclusion)
        return ResultFactory.getSuccessResult(exclusion)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun deactivate(exclusion: ProviderExclusion): Result<ProviderExclusion> {
        var providerExclusion = repo.findById(exclusion.id!!)
        providerExclusion.ifPresent {
            it.apply {
                processed = false
                status = Status.INACTIVE
            }
            repo.save(it)
        }
        return ResultFactory.getSuccessResult(providerExclusion.get())
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun process(category: Category): Result<MutableList<ProviderExclusion>> {
        val exclusions = repo.findByCategory(category)
        for (exclusion in exclusions) {
            exclusion.apply { processed = true; processedTime = LocalDateTime.now() }
        }
        //TODO send to provider exclusion kafka topic
        repo.saveAll(exclusions)
        return ResultFactory.getSuccessResult("Successfully processed the exclusions")
    }

    override fun findExclusionsByProvider(providerId: Long): Result<MutableList<ProviderExclusion>> {
        val provider = providerRepo.findById(providerId)
        if (provider.isPresent) {
            val results = repo.findByProvider(provider.get())
            return ResultFactory.getSuccessResult(results)
        }
        return ResultFactory.getFailResult("No provider with ID $providerId was found")

    }

    override fun findExclusionsByCategory(categoryId: Long, page: Int, size: Int): Result<Page<ProviderExclusion>> {
        val category = categoryRepo.findById(categoryId)
        if (category.isPresent) {
            val request = PageRequest.of(page-1,size)
            val results = repo.findByCategory(category.get(),request)
            return ResultFactory.getSuccessResult(results)
        }

        return ResultFactory.getFailResult("No category with ID $categoryId was found")
    }

    override fun findExclusionsByCategoryAndProvider(
        categoryId: Long,
        providerId: Long
    ): Result<Boolean> {
        val category = categoryRepo.findById(categoryId)
        val provider = providerRepo.findById(providerId)

        if (category.isEmpty) return ResultFactory.getFailResult("No category with ID $categoryId was found")
        if (provider.isEmpty) return ResultFactory.getFailResult("No provider with ID $providerId was found")

        val optional = repo.findByCategoryAndProvider(category.get(), provider.get())
        return if (optional.isPresent) {
            ResultFactory.getSuccessResult(
                data = true,
                msg = "An exclusion exists at ${provider.get().name} for category ${category.get
                    ().name}"
            )
        }else{
            ResultFactory.getFailResult(
                data = false,
                msg = "No Exclusion exists at ${provider.get().name} for category ${category.get
                    ().name}"
            )
        }

    }
}