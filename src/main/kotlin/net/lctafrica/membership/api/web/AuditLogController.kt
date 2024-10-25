package net.lctafrica.membership.api.web

import net.lctafrica.membership.api.dtos.AuditLogDTO
import net.lctafrica.membership.api.dtos.PayerDTO
import net.lctafrica.membership.api.service.*
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/v1/audit")
@CrossOrigin("*","*")
class AuditLogController(
	val auditLogService: AuditLogService
) : AbstractController() {

	@PostMapping(value = ["/save"], produces = ["application/json"])
	fun saveLog( @RequestBody dto: AuditLogDTO) = auditLogService.saveLog(dto)

}