package net.lctafrica.membership.api.web

import javax.validation.Valid
import net.lctafrica.membership.api.dtos.AllocateDeviceRequestDTO
import net.lctafrica.membership.api.dtos.DeviceRequestDTO
import net.lctafrica.membership.api.service.IDeviceManagementService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/device")
class DeviceController(val service: IDeviceManagementService) : AbstractController() {

    @PostMapping(value = ["/register"], produces = ["application/json"])
    fun registerDevice(@Valid @RequestBody dto: DeviceRequestDTO) = service.registerDevice(dto)

    @PostMapping(value = ["/allocate"], produces = ["application/json"])
    fun allocateDevice(@Valid @RequestBody dto: AllocateDeviceRequestDTO) = service.allocateDevice(dto)

    @GetMapping(value = ["/allocation"], produces = ["application/json"])
    fun getDeviceAllocation(
        @RequestParam(name = "providerId") providerId: Long,
        @RequestParam(name = "deviceId") deviceId: String,
        @RequestParam(name = "imei1") imei1: String?,
        @RequestParam(name = "imei2") imei2: String?
    ) =
        service.getDeviceAllocation(providerId, deviceId, imei1, imei2)

}