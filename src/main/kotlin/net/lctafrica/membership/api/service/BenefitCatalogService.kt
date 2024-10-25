package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.BenefitCatalog
import net.lctafrica.membership.api.domain.BenefitCatalogRepository
import net.lctafrica.membership.api.domain.ServiceGroup
import net.lctafrica.membership.api.dtos.BenefitCatalogDTO
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors

@Service("benefitCatalogService")
@Transactional
class BenefitCatalogService(private val repo: BenefitCatalogRepository) : IBenefitCatalogService {

    @Transactional(rollbackFor = [Exception::class])
    override fun addBenefitCatalog(dto: BenefitCatalogDTO): Result<BenefitCatalog> {
        var addition = BenefitCatalog(id = null, code = dto.code, name = dto.name, serviceGroup = dto.serviceGroup)
        repo.save(addition)
        return ResultFactory.getSuccessResult(addition)
    }

    @Transactional(readOnly = true)
    override fun searchByName(search: String, page: Int, size: Int): Result<Page<BenefitCatalog>> {
        val request = PageRequest.of(page-1,size)
        val results = repo.findByNameLike(search = "%${search}%",request)
        return ResultFactory.getSuccessResult(results)
    }

    @Transactional(readOnly = true)
    override fun getAllCatalogBenefits(page: Int, size: Int): Result<Page<BenefitCatalog>> {
        val all = repo.findAll()
        val request = PageRequest.of(page - 1, size)
        val pages = repo.findAll(request)
        return ResultFactory.getSuccessResult(pages)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun batchAddition(services: List<BenefitCatalogDTO>): Result<MutableList<BenefitCatalog>> {
        var additionList = services.stream().map {
            BenefitCatalog(code = it.code, name = it.name, id = null, serviceGroup = it.serviceGroup)
        }.collect(Collectors.toList())
        repo.saveAll(additionList)
        return ResultFactory.getSuccessResult(additionList)
    }

    @Transactional(readOnly = true)
    override fun findByServiceGroup(serviceGroup: ServiceGroup, page: Int, size: Int): Result<Page<BenefitCatalog>> {
        val request = PageRequest.of(page-1, size)
        val catalog = repo.findByServiceGroup(serviceGroup,request)
        return ResultFactory.getSuccessResult(catalog)
    }
}
