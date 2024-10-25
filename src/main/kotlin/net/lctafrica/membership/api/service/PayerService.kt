package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.dtos.*
import net.lctafrica.membership.api.util.Patterns
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.stream.Collectors


@Service("payerService")
@Transactional
class PayerService(
    private val repo: PayerRepository,
    private val benefitCatalogRepo: BenefitCatalogRepository,
    private val benefitMappingRepo: PayerBenefitMappingRepository,
    private val providerRepo: ProviderRepository,
    private val providerMappingRepo: PayerProviderMappingRepository,
    private val benefitRepository: BenefitRepository,
    private val categoryRepository: CategoryRepository
) : IPayerService {


    @Transactional(readOnly = true)
    override fun findAll(): Result<MutableList<Payer>> {
        val allPayers = repo.findAll()
        return ResultFactory.getSuccessResult(allPayers)
    }

    @Transactional(readOnly = true)
    override fun findAdministrators(): Result<MutableList<Payer>> {
        val types: List<PayerType> = listOf(
            PayerType.INTERMEDIARY, PayerType.UNDERWRITER,
            PayerType.CORPORATE
        )
        val admins = repo.findAdmins(types)
        return ResultFactory.getSuccessResult(admins);
    }


    @Transactional(readOnly = false, rollbackFor = [Exception::class])
    override fun addPayer(dto: PayerDTO): Result<Payer> {
        var stored = repo.findByNameIgnoreCase(name = dto.name.trim())
        return if (stored.isPresent) {
            ResultFactory.getFailResult<Payer>("Payer ${dto.name} already exists")
        } else {

            val saved = repo.save(
                Payer(
                    name = dto.name.trim().replace(Patterns.doublespace, " "),
                    contact = dto.contact.trim().replace(Patterns.doublespace, " "),
                    type = dto.type,
                    //plans = mutableListOf(),
                    benefits = mutableListOf()
                )
            )
            ResultFactory.getSuccessResult(saved)
        }
    }

    @Transactional(readOnly = true)
    override fun findByType(type: PayerType): Result<MutableList<Payer>> {
        val payers = repo.findByType(type)
        return ResultFactory.getSuccessResult(payers)
    }

    override fun findById(payerId: Long): Result<Payer> {
        val payer = repo.findById(payerId).get();
        return ResultFactory.getSuccessResult(payer)
    }


    @Transactional(rollbackFor = [Exception::class])
    override fun addBenefitMapping(dto: PayerBenefitMappingDTO): Result<PayerBenefitMapping> {

        val payer = repo.findById(dto.payerId)
        if (payer.isEmpty) return ResultFactory.getFailResult("No payer with ID ${dto.payerId} was found")
        val benefit = benefitCatalogRepo.findById(dto.benefitCatalogId)
        if (benefit.isEmpty) return ResultFactory.getFailResult("No benefit catalog with ID ${dto.benefitCatalogId} was found")

        var mapping = PayerBenefitMapping(benefit = benefit.get(), payer = payer.get(), code = dto.code)
        benefitMappingRepo.save(mapping)
        return ResultFactory.getSuccessResult(mapping)
    }

    @Transactional(readOnly = true)
    override fun findBenefitMapping(payerId: Long): Result<MutableList<PayerBenefitMapping>> {
        val payer = repo.findById(payerId)
        if (payer.isEmpty) return ResultFactory.getFailResult("No payer with ID $payerId was found")
        val mappings = benefitMappingRepo.findByPayer(payer.get())
        return ResultFactory.getSuccessResult(mappings)
    }

    @Transactional(readOnly = true)
    override fun findBenefitCode(payerId: String, benefitId: String): Result<PayerBenefitCodeMappingDTO?> {

        val payer = repo.findById(java.lang.Long.valueOf(payerId))
        if (payer.isEmpty) return ResultFactory.getFailResult("No payer with ID ${payerId} was found")

        val byBenefitId = benefitRepository.findByBenefitId(benefitId)
        if (byBenefitId.isEmpty) return ResultFactory.getFailResult(
            "No Benefit with ID " +
                    "$benefitId was found"
        )

        val benefitRef = byBenefitId.get().benefitRef.id!!

        val mappings = benefitMappingRepo.findByPayerIdAndBenefitId(
            java.lang.Long.valueOf(payerId),
            java.lang.Long.valueOf(benefitRef)
        )

        return ResultFactory.getSuccessResult(mappings)

    }

    override fun findPayerProviderMapping(payerId: Long, providerId: Long): Result<PayerProviderMap> {
        val payer = repo.findById(payerId)
        if (payer.isEmpty) return ResultFactory.getFailResult("No payer with ID $payerId was found")
        val provider = providerRepo.findById(providerId)
        if (provider.isEmpty) return ResultFactory.getFailResult("No provider with ID $providerId was found")
        val mapping = providerMappingRepo.findByPayerIdAndProviderId(
            payerId,
            providerId
        )

        return if (mapping.isPresent) {
            ResultFactory.getSuccessResult(
                PayerProviderMap(payerId = payerId, providerId = providerId, code = mapping.get().code!!)
            )
        } else {
            ResultFactory.getFailResult("No mapping for ${provider.get()} was found for ${payer.get()}")
        }

    }

    override fun findPayerProviderAndBenefitMappings(providerId: Long, payerId: Long, benefitId: Long, categoryId: Long ): Result<PayerMappings> {
        val provider = providerMappingRepo.findByPayerIdAndProviderId(payerId, providerId)
        val category: Optional<Category> = categoryRepository.findById(categoryId)
        val service = benefitCatalogRepo.findByBenefitId(benefitId)
        val benefit = benefitMappingRepo.findByPayerIdAndBenefitId(payerId, service.serviceId)
        val payer = repo.findById(payerId)
        return if (provider.isEmpty) {
            ResultFactory.getFailResult("No mapping was found for (PROVIDER: $providerId, PAYER: $payerId). Please contact the benefit payer")
        } else {
            val mappings = PayerMappings(
                payerId = payerId,
                payerName = payer.get().name,
                benefitCode = benefit?.code,
                serviceId = service.serviceId,
                serviceGroup = service.serviceGroup,
                providerCode = provider.get().code,
                providerName = provider.get().provider?.name,
                schemeName = category.get().policy.plan.name,
                policyStartDate = category.get().policy.startDate,
                policyEndDate = category.get().policy.endDate
            )
            ResultFactory.getSuccessResult(mappings)
        }

    }

    data class PayerProviderMap(
        val payerId: Long,
        val providerId: Long,
        val code: String
    )


    @Transactional(rollbackFor = [Exception::class])
    override fun addProviderMapping(dto: PayerProviderMappingDTO): Result<PayerProviderMapping> {
        val payer = repo.findById(dto.payerId)
        if (payer.isEmpty) return ResultFactory.getFailResult("No payer with ID ${dto.payerId} was found")
        val provider = providerRepo.findById(dto.providerId)
        if (provider.isEmpty) return ResultFactory.getFailResult("No provider with ID ${dto.providerId} was found")

        return if(providerMappingRepo.findByPayerIdAndProviderId(dto.payerId,dto.providerId).isPresent){
            ResultFactory.getFailResult(msg = "Payer Provider Mapping already Exists")
        }else{
            val mapping = PayerProviderMapping(payer = payer.get(), provider = provider.get(), code = dto.code)
            providerMappingRepo.save(mapping)
            ResultFactory.getSuccessResult(mapping)
        }


    }

    @Transactional(readOnly = true)
    override fun findProviderMapping(payerId: Long, page: Int, size: Int): Result<Page<PayerProviderMapping>> {
        val payer = repo.findById(payerId)
        if (payer.isEmpty) return ResultFactory.getFailResult("No payer with ID $payerId was found")
        val request = PageRequest.of(page - 1, size)
        val mappings = providerMappingRepo.findByPayer(payer.get(), request)


        return if (mappings.hasContent() && mappings.content.isNotEmpty()) {

            val res: List<PayerProviderMapping> = mappings.get().filter {
                !it.code.isNullOrBlank() && !it.code.isNullOrEmpty()
            }.collect(Collectors.toList())

            val payerProviderMap: Page<PayerProviderMapping> = PageImpl(res)


            ResultFactory.getSuccessResult(
                data = payerProviderMap, msg = "Mapping " +
                        "Successfully found"
            )
        } else {
            ResultFactory.getFailResult(msg = "No  mapping exists")
        }

    }


}