package net.lctafrica.membership.api.service

import java.util.stream.Collectors
import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.dtos.MultiProviderUpdateDTO
import net.lctafrica.membership.api.dtos.MultipleWhiteListDTO
import net.lctafrica.membership.api.dtos.NearbyProvidersDTO
import net.lctafrica.membership.api.dtos.ProviderDTO
import net.lctafrica.membership.api.dtos.ProviderUpdateDTO
import net.lctafrica.membership.api.dtos.WhiteListDTO
import net.lctafrica.membership.api.helper.ExcelHelper
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service("providerService")
@Transactional
class ProviderService(
	private val repo: ProviderRepository,
	private val countryRepo: CountryRepository,
	private val regionRepo: RegionRepository,
	private val whiteListRepo: WhiteListRepository,
	private val benefitCatalogRepo: BenefitCatalogRepository,
	private val providerRepository: ProviderRepository,
	private val payerProviderMappingRepository: PayerProviderMappingRepository,
	private val payerRepository: PayerRepository,
	@Lazy @Autowired val excelHelper: ExcelHelper,
) : IProviderService {

	override fun findById(providerId: Long): Result<Provider> {
		val optional = repo.findById(providerId)
		return when {
			optional.isPresent -> {
				ResultFactory.getSuccessResult(optional.get())
			}
			else -> {
				ResultFactory.getFailResult("Invalid Provider Id")
			}
		}
	}

	@Transactional(rollbackFor = [Exception::class])
	override fun addProvider(dto: ProviderDTO): Result<Provider> {
		var mainFacility: Provider? = null
		repo.findById(dto.mainFacilityId).ifPresent { mainFacility = it }
		var country = countryRepo.findByNameIgnoreCase(dto.country.trim())
		var region: Region = country?.let { c ->
			return@let regionRepo.findByCountryAndNameIgnoreCase(c, dto.region.trim())
		} ?: return ResultFactory.getFailResult("No such region was found")


		if (dto.providerId !== null) {
			val optional = repo.findById(dto.providerId)

			val provider = optional.get()
			provider.apply {
				this.name = dto.name
				//county = dto.county,
				this.billsOnHmis = dto.billsOnHmis!!
				this.billsOnDevice = dto.billsOnDevice!!
				this.billsOnPortal = dto.billsOnPoral!!
				this.latitude = dto.latitude
				this.longitude = dto.longitude
				this.tier = dto.tier
				this.region = region
				this.mainFacility = mainFacility

				this.verificationType = VerificationType.valueOf(dto.verificationType.toString())
			}
			repo.save(provider)
			return ResultFactory.getSuccessResult(provider)

		} else {
			val provider = Provider(
				id = null,
				name = dto.name,
				//county = dto.county,
				latitude = dto.latitude,
				longitude = dto.longitude,
				tier = dto.tier,
				mainFacility = mainFacility,
				region = region
			)
			repo.save(provider)
			return ResultFactory.getSuccessResult(provider)
		}


	}

	@Transactional
	override fun findByTier(tier: ProviderTier, page: Int, size: Int): Result<Page<Provider>> {
		val request = PageRequest.of(page - 1, size)
		val providers = repo.findByTier(tier, request)
		return ResultFactory.getSuccessResult(providers)
	}

	override fun findByName(name: String, page: Int, size: Int): Result<Page<Provider>> {
		val request = PageRequest.of(page - 1, size)
		val providers = repo.searchByName(name, request)
		return ResultFactory.getSuccessResult(providers)
	}

	@Transactional(readOnly = true)
	override fun findAllProviders(page: Int, size: Int): Result<Page<Provider>> {
		val all = repo.findAll()
		val request = PageRequest.of(page - 1, size)
		val pages = repo.findAll(request)
		return ResultFactory.getSuccessResult(pages)
	}

	@Transactional(readOnly = true)
	override fun findByRegion(regionId: Long, page: Int, size: Int): Result<Page<Provider>> {
		val region = regionRepo.findById(regionId)
		if (region.isEmpty) return ResultFactory.getFailResult("No such region was found")
		val pageRequest = PageRequest.of(page - 1, size)
		region.get().let { r ->
			val providers = repo.findByRegion(r, pageRequest)
			return ResultFactory.getSuccessResult(providers)
		}
	}

	@Transactional(readOnly = true)
	override fun findWhiteListByProvider(
		providerId: Long,
		page: Int,
		size: Int
	): Result<Page<Whitelist>> {
		val provider = repo.findById(providerId)
		val pageRequest = PageRequest.of(page - 1, size)
		if (provider.isPresent) {
			val whitelist = whiteListRepo.findByProvider(provider.get(), pageRequest)
			return ResultFactory.getSuccessResult(whitelist)
		}
		return ResultFactory.getFailResult("No provider with ID $providerId was found")
	}

	@Transactional(readOnly = true)
	override fun findWhiteListByBenefit(
		benefitId: Long,
		page: Int,
		size: Int
	): Result<Page<Whitelist>> {
		val benefit = benefitCatalogRepo.findById(benefitId)
		val pageRequest = PageRequest.of(page - 1, size)
		if (benefit.isPresent) {
			val whitelist = whiteListRepo.findByBenefit(benefit.get(), pageRequest)
			return ResultFactory.getSuccessResult(whitelist)
		}
		return ResultFactory.getFailResult("No benefit with ID $benefitId was found")
	}

	@Transactional(rollbackFor = [Exception::class])
	override fun addWhitelist(dto: WhiteListDTO): Result<Whitelist> {
		val benefit = benefitCatalogRepo.findById(dto.benefitId)
		val provider = repo.findById(dto.providerId)
		return if (benefit.isPresent) {
			if (provider.isPresent) {
				val white = Whitelist(
					benefit = benefit.get(),
					provider = provider.get()
				)
				whiteListRepo.save(white)
				ResultFactory.getSuccessResult(white)
			} else {
				ResultFactory.getFailResult("No provider with ID ${dto.providerId} was found")
			}
		} else {
			ResultFactory.getFailResult("No benefit with ID ${dto.benefitId} was found")
		}
	}

	@Transactional(rollbackFor = [Exception::class])
	override fun addMultipleMappings(
		providerId: Long,
		dto: MultipleWhiteListDTO
	): Result<MutableList<Whitelist>> {
		val provider = repo.findById(providerId)
		val benefits = benefitCatalogRepo.findByIds(dto.benefitIds)
		return if (provider.isPresent) {
			if (benefits.size > 0) {
				val mappings = mutableListOf<Whitelist>()
				benefits.forEach { b ->
					val white = Whitelist(
						benefit = b,
						provider = provider.get()
					)
					mappings.add(white)
				}
				whiteListRepo.saveAll(mappings)
				ResultFactory.getSuccessResult(mappings)
			} else {
				ResultFactory.getFailResult("No benefit in the catalog matched the identifier(s) provided")
			}
		} else {
			ResultFactory.getFailResult("No provider with ID $providerId was found")
		}
	}

	override fun saveProvidersFromFile(file: MultipartFile): Result<List<Provider>> {

		val importedProviders: List<Provider>? = excelHelper.excelToproviders(file.inputStream)
		if (importedProviders != null) {
			providerRepository.saveAll(importedProviders.toList())
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved providers")
	}

	@Transactional(rollbackFor = [Exception::class])
	override fun saveProviderMappingFromFile(file: MultipartFile): Result<List<PayerProviderMapping>> {
		val importedProviderMapping: List<PayerProviderMapping>? =
			excelHelper.excelToPayerProviderMapping(file.inputStream)
		if (importedProviderMapping != null) {
			payerProviderMappingRepository.saveAll(importedProviderMapping.toList())
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved provider mapping")
	}

	override fun getNearbyProviders(beneficiaryId:Long, latitude:Double, longitude:Double, radius:Double, page:Int, size:Int): Result<List<NearbyProvidersDTO>> {
		val payers = payerRepository.findPayerByBeneficiaryId(beneficiaryId)
		val payerIds = payers.stream().map(Payer::id).distinct().collect(Collectors.toList())
		return ResultFactory.getSuccessResult(repo.findNearbyProviders(latitude,longitude,radius,page -1,size, payerIds))
	}

	override fun updateProvider(dto: ProviderUpdateDTO): Result<Provider> {
		val providerOpt = repo.findById(dto.providerId)
		if(providerOpt.isPresent) {
			val provider = providerOpt.get()
			dto.latitude?.let {
				provider.latitude = it
			}
			dto.longitude?.let {
				provider.longitude = it
			}
			return ResultFactory.getSuccessResult(repo.save(provider))
		}
		return ResultFactory.getFailResult("Invalid Provider Id")
	}

	override fun multiProviderUpdate(dto: MultiProviderUpdateDTO): Result<Boolean> {
		var countErrors =0
		dto.providers.forEach { providerDto ->
			val providerOpt = repo.findById(providerDto.providerId)
			if(providerOpt.isPresent) {

				val provider = providerOpt.get()
				providerDto.latitude?.let {
					provider.latitude = it
				}
				providerDto.longitude?.let {
					provider.longitude = it
				}
				repo.save(provider)
			}else{
				countErrors++
			}
		}
		return ResultFactory.getSuccessResult("Successfully Updated with $countErrors errors")
	}


}
