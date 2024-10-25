package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.dtos.AuditLogDTO
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service("auditLogService")
@Transactional
class AuditLogService(
	private val auditLogRepo: AuditLogRepo,
	private val beneficiaryRepository: BeneficiaryRepository,
) : IAuditLogService {

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun saveLog(auditLogDTO: AuditLogDTO): Result<AuditLog> {

		if (auditLogDTO.beneficiaryId !== null) {
			val beneficiary = beneficiaryRepository.findById(auditLogDTO.beneficiaryId!!)
			if (beneficiary.isEmpty)
				return ResultFactory.getFailResult(
					msg = "Beneficiary with id " +
							"${auditLogDTO.beneficiaryId} not found "
				)
			val audit = AuditLog(
				data = auditLogDTO.data,
				user = auditLogDTO.user,
				action = auditLogDTO.action,
				beneficiaryId = beneficiary.get(),
				organisation = auditLogDTO.organisation,
				reason = auditLogDTO.reason,
				time = LocalDate.now(),
				memberNumber = auditLogDTO.memberNumber,
				type = ChangeLogType.valueOf(auditLogDTO.type.toString())
			)

			val auditlogResponse = auditLogRepo.save(audit)
			return ResultFactory.getSuccessResult(data = auditlogResponse)
		} else {
			val audit = AuditLog(
				data = auditLogDTO.data,
				user = auditLogDTO.user,
				action = auditLogDTO.action,
				beneficiaryId = null,
				organisation = auditLogDTO.organisation,
				reason = auditLogDTO.reason,
				time = LocalDate.now(),
				memberNumber = auditLogDTO.memberNumber,
				type = ChangeLogType.valueOf(auditLogDTO.type.toString())
			)

			val auditlogResponse = auditLogRepo.save(audit)
			return ResultFactory.getSuccessResult(data = auditlogResponse)
		}


	}

}