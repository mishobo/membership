package net.lctafrica.membership.api.service

import java.time.LocalDateTime
import net.lctafrica.membership.api.domain.AllocationStatus
import net.lctafrica.membership.api.domain.DeviceAllocation
import net.lctafrica.membership.api.domain.DeviceAllocationRepo
import net.lctafrica.membership.api.domain.DeviceCatalog
import net.lctafrica.membership.api.domain.DeviceCatalogRepo
import net.lctafrica.membership.api.domain.ProviderRepository
import net.lctafrica.membership.api.domain.Status
import net.lctafrica.membership.api.dtos.AllocateDeviceRequestDTO
import net.lctafrica.membership.api.dtos.DeviceRequestDTO
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service("deviceManagementService")
class DeviceManagementService(
    private val deviceCatalogRepo: DeviceCatalogRepo,
    private val deviceAllocationRepo: DeviceAllocationRepo,
    private val providerRepo: ProviderRepository
) : IDeviceManagementService {

    override fun registerDevice(dto: DeviceRequestDTO): Result<DeviceCatalog> {
        val deviceCheck = deviceCatalogRepo.findByDeviceIdAndImei(
            deviceId = dto.deviceId,
            firstImei = dto.imei1,
            secondImei = dto.imei2
        )
        if (deviceCheck.isNotEmpty()) {
            return ResultFactory.getFailResult("Device is already registered")
        }

        val catalog = DeviceCatalog(
            deviceId = dto.deviceId,
            firstImei = dto.imei1,
            secondImei = dto.imei2,
            status = Status.ACTIVE
        )
        deviceCatalogRepo.save(catalog)
        return ResultFactory.getSuccessResult(catalog)
    }

    override fun allocateDevice(dto: AllocateDeviceRequestDTO): Result<Boolean> {
        val catalog = deviceCatalogRepo.findById(dto.deviceCatalogId)
        if(catalog.isPresent){
            val provider = providerRepo.findById(dto.providerId)
            if(provider.isPresent){
                val allocationCheck = deviceAllocationRepo.findByDeviceCatalogAndStatus(
                    deviceCatalog = catalog.get(),
                    AllocationStatus.ALLOCATED
                )
                if(allocationCheck.isPresent){
                    return ResultFactory.getFailResult("Device is already allocated")
                }
                val allocation = DeviceAllocation(
                    deviceCatalog = catalog.get(),
                    provider = provider.get(),
                    status = AllocationStatus.ALLOCATED
                )
                deviceAllocationRepo.save(allocation)
                return ResultFactory.getSuccessResult(true)
            }
            return ResultFactory.getFailResult("Provider with provided id not found")
        }
        return ResultFactory.getFailResult("Device with provided id does not exist")
    }

    @Transactional(readOnly = true)
    override fun getDeviceAllocation(
        providerId: Long,
        deviceId: String,
        imei1: String?,
        imei2: String?
    ): Result<DeviceAllocation> {
        val deviceOpt = deviceCatalogRepo.findByDeviceId(deviceId)
        if (deviceOpt.isPresent) {
            val deviceCatalog = deviceOpt.get()
            if (deviceCatalog.status == Status.ACTIVE) {
                val deviceAllocation =
                    deviceAllocationRepo.findByDeviceCatalogAndStatus(deviceCatalog, AllocationStatus.ALLOCATED)
                return if (deviceAllocation.isPresent) {
                    val allocation = deviceAllocation.get()
                    val cashierProviderOpt = providerRepo.findById(providerId)
                    if(!cashierProviderOpt.isPresent){
                        return  ResultFactory.getFailResult("Provider Id doesn't exist")
                    }
                    val cashierProvider = cashierProviderOpt.get()
                    val deviceProvider = providerRepo.findById(allocation.provider.id ?: 0).get()

                    val deviceMainFacilityId = deviceProvider.mainFacility?.id ?: -1
                    val cashierMainFacilityId = cashierProvider.mainFacility?.id ?: -2

                    if (providerId == deviceProvider.id || providerId == deviceMainFacilityId ||
                        cashierMainFacilityId == deviceProvider.id ||
                        cashierMainFacilityId == deviceMainFacilityId
                    ) {
                        ResultFactory.getSuccessResult(allocation)
                    } else {
                        ResultFactory.getFailResult("Provider Id and Device mismatch")
                    }
                } else {
                    ResultFactory.getFailResult("Device has not been allocated")
                }
            } else {
                return ResultFactory.getFailResult("Device status is ${deviceCatalog.status}")
            }
        }
        return ResultFactory.getFailResult("Provided Device Id is not yet registered")
    }

}