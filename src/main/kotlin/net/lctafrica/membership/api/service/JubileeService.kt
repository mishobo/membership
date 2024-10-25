package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.dtos.*
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Suppress("DEPRECATION")
@Service("jubileeService")
@Transactional
class JubileeService(
	private val repo: PolicyRepository,
	private val planRepo: PlanRepository,
	private val categoryRepository: CategoryRepository,
	private val providerRepository: ProviderRepository,
	private val providerMappingRepo: PayerProviderMappingRepository,
	private val benefitRepository: BenefitRepository,
	private val beneficiaryRepository: BeneficiaryRepository
) :IJubileeService {
	override fun findPolicyIdAndDate(categoryId:Long,memberNumber:String):
			Result<JubileeResponseMemberDTO?> {
		val category =  categoryRepository.findById(categoryId)

		if(category.isEmpty) {
			return ResultFactory.getFailResult("No Category with id $categoryId")
		} else{
			val categoryObject = category.get()
			val beneficiary = beneficiaryRepository.findByCategoryAndBeneficiary(categoryObject,
				memberNumber).get()

			return if(beneficiary.jicEntityId == null){
				ResultFactory.getFailResult(data = null, msg = " Jubilee ENTITY ID IS NULL")
			}else{
				val jubileeMember = JubileeResponseMemberDTO(
					memberFullName = beneficiary.name,
					memberActisureId = beneficiary.jicEntityId!!,
					policyEffectiveDate = category.get().policy.startDate.toString(),
					policyPayerCode = category.get().policyPayerCode!!,
					schemeName = category.get().policy.plan.name)
				ResultFactory.getSuccessResult(data = jubileeMember, msg = "policy " +
						"found successfully")
			}

		}

	}

	override fun findProviderCode(providerId:Long,payerId:Long):
			Result<JubileePayerProviderCodeMappingDTO> {
		val payer = repo.findById(payerId)
		if (payer.isEmpty) return ResultFactory.getFailResult("No such with Id $payerId Payer was found")
		val provider = providerRepository.findById(providerId)
		if (provider.isEmpty) return ResultFactory.getFailResult("No provider with ID$providerId " +
				"was found")

		val providerPayerMapping = providerMappingRepo.findByProviderIdAndPayerId(providerId,payerId)
		return if (providerPayerMapping.isPresent && !providerPayerMapping.get().code.isNullOrBlank()
			&& providerPayerMapping.get().code!!.isNotEmpty()
		){
			ResultFactory.getSuccessResult(data = providerPayerMapping.get(), msg = "Mapping " +
					"Successfully found")
		}else{
			ResultFactory.getFailResult(msg = "No  mapping exists")
		}

	}

	override fun findBenefit(benefitId:Long,policyId:Long): Result<JubileeResponseBenefitNameDTO?> {

		val byBenefitId = benefitRepository.findByBenefitId(benefitId.toString())
		if (byBenefitId.isEmpty) {
			return ResultFactory.getFailResult("No Benefit with ID $benefitId was found")
		}else {
			var jubileeNames: JubileeResponseBenefitNameDTO? = null;
			val benefitName = byBenefitId.get().name
			when {
				benefitName.toUpperCase().contains("OUTPATIENT") -> {
					val subBenefitName = "Illness Outpatient "
					jubileeNames = JubileeResponseBenefitNameDTO(
						benefit = "Outpatient Enhanced II Shared ",
						subBenefit = subBenefitName
					)
				}
				benefitName.toUpperCase().contains("INPATIENT") -> {
					val subBenefitName = "Illness Inpatient "
					jubileeNames = JubileeResponseBenefitNameDTO(
						benefit = benefitName,
						subBenefit = subBenefitName
					)

				}
				benefitName.toUpperCase().contains("DENTAL")  -> {
					val subBenefitName = "Dental treatment"
					jubileeNames = JubileeResponseBenefitNameDTO(
						benefit = benefitName,
						subBenefit = subBenefitName
					)

				}
				benefitName.toUpperCase().contains("OPTICAL") -> {
					val subBenefitName = "Optical treatment"
					jubileeNames = JubileeResponseBenefitNameDTO(
						benefit = benefitName,
						subBenefit = subBenefitName
					)

				}
				benefitName.toUpperCase().contains("MATERNITY") -> {
					val subBenefitName = "Normal Delivery "
					jubileeNames = JubileeResponseBenefitNameDTO(
						benefit = benefitName,
						subBenefit = subBenefitName
					)

				}
			}

			return ResultFactory.getSuccessResult(
				data = jubileeNames,
				msg = "Benefit successfully found"
			)
		}
	}
}