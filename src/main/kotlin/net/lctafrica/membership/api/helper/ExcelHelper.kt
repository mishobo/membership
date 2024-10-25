package net.lctafrica.membership.api.helper

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.gson
import net.lctafrica.membership.api.service.IPayerService
import net.lctafrica.membership.api.service.IProviderService
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.client.WebClient
import java.io.IOException
import java.io.InputStream

@Component
class ExcelHelper(val payerService: IPayerService,
                  val providerService: IProviderService
) {
	var TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	var HEADERs = arrayOf("Code", "Title")
	var SHEET = "ICD10CODES"
	var PROVIDERS = "PROVIDERS"
	var MAPPING = "MAPPING"
	var SHEET_DRUGS = "medical_drugs"
	var SHEET_LAB = "lab"
	var SHEET_RADIOLOGY = "radiology"
	var SHEET_PROCEDURES = "medical_procedures"
	var SHEET_OTHER_BENEFIT_DETAIL = "OTHER"

	@Value("\${lct-africa.member.url}")
	lateinit var memberUrl: String

	fun hasExcelFormat(file: MultipartFile): Boolean {
		return TYPE == file.contentType
	}

//	 fun icd10codesToExcel(icd10codes: List<Icd10code?>): ByteArrayInputStream? {
//		try {
//			XSSFWorkbook().use { workbook ->
//				ByteArrayOutputStream().use { out ->
//					val sheet = workbook.createSheet(SHEET)
//
//					// Header
//					val headerRow: Row = sheet.createRow(0)
//					for (col in HEADERs.indices) {
//						val cell: Cell = headerRow.createCell(col)
//						cell.setCellValue(HEADERs[col])
//					}
//					var rowIdx = 1
//					for (icd10code in icd10codes) {
//						val row: Row = sheet.createRow(rowIdx++)
//						if (icd10code != null) {
//							row.createCell(0).setCellValue(icd10code.code.toString())
//							row.createCell(1).setCellValue(icd10code.title.toString())
//						}
//					}
//					workbook.write(out)
//					return ByteArrayInputStream(out.toByteArray())
//				}
//			}
//		} catch (e: IOException) {
//			throw RuntimeException("fail to import data to Excel file: " + e.message)
//		}
//	}

	fun excelToproviders(`is`: InputStream?): List<Provider>? {
		return try {
			val workbook: Workbook = XSSFWorkbook(`is`)
			var row: Row
			var cell: Cell?
			val sheet = workbook.getSheet(PROVIDERS)
			val rows: Iterator<Row> = sheet.iterator()
			val providers: MutableList<Provider> = ArrayList<Provider>()
			var rowNumber = 0
			val formatter = DataFormatter()
			var style:CellStyle  = workbook.createCellStyle();
			val format = workbook.createDataFormat()
			style = workbook.createCellStyle();
			style.setDataFormat(format.getFormat("0.0"));
			 //precision only up to 15 significant
			// digits

			while (rows.hasNext()) {
				val currentRow: Row = rows.next()

				// skip header
				if (rowNumber == 0) {
					rowNumber++
					continue
				}
				val cellsInRow: Iterator<Cell> = currentRow.iterator()
				val exprovider = Provider()
				var cellIdx = 0
				while (cellsInRow.hasNext()) {
					val currentCell: Cell = cellsInRow.next()



					when (cellIdx) {
						0 -> exprovider.name = currentCell.stringCellValue
						1 -> exprovider.tier = ProviderTier.valueOf(currentCell.stringCellValue)
						2 -> exprovider.region = findRegionById(currentCell.numericCellValue
							.toLong())

						else -> {}
					}
					cellIdx++
				}
				providers.add(exprovider)
			}
			workbook.close()
			providers
		} catch (e: IOException) {
			throw RuntimeException("fail to parse Excel file: " + e.message)
		}
	}

	fun findRegionById(regionId:Long): Region {
		val memberClient = WebClient.builder()
			.baseUrl(memberUrl).build()

		val regionResponse = memberClient
			.get()
			.uri { u ->
				u
					.path("/api/v1/country/region/$regionId")
					.build()
			}
			.retrieve()
			.bodyToMono(String::class.java)
			.block()
		val regionRemoteResponse = gson.fromJson(regionResponse, RegionResponse::class.java)
		return regionRemoteResponse.data
	}

	fun findProviderById(providerId:Long): Provider? {
		println(providerId)
		return	providerService.findById(providerId).data
	}
	fun findPayerById(payerId:Long): Payer? {
		return if (payerService.findById(payerId).data !== null)
			payerService.findById(payerId).data
		else {
			null
		}
	}

	fun excelToPayerProviderMapping(`is`: InputStream?): List<PayerProviderMapping>? {
		return try {
			val workbook: Workbook = XSSFWorkbook(`is`)
			var row: Row
			var cell: Cell?
			val sheet = workbook.getSheet(MAPPING)
//			val sheet = workbook.getSheet(DEVICE)
			val rows: Iterator<Row> = sheet.iterator()
			val payerProviderMappingList: MutableList<PayerProviderMapping> =
				ArrayList<PayerProviderMapping>()
			var rowNumber = 0
			val formatter = DataFormatter()

			//precision only up to 15 significant
			// digits

			while (rows.hasNext()) {
				val currentRow: Row = rows.next()

				// skip header
				if (rowNumber == 0) {
					rowNumber++
					continue
				}
				val cellsInRow: Iterator<Cell> = currentRow.iterator()
				val payerProviderMapping = PayerProviderMapping()
				var cellIdx = 0
				while (cellsInRow.hasNext()) {
					val currentCell: Cell = cellsInRow.next()

					when (cellIdx) {
//						KENGEN
						0 -> payerProviderMapping.provider = findProviderById(currentCell.numericCellValue.toLong())!!
						1 ->  payerProviderMapping.payer = findPayerById(currentCell.numericCellValue.toLong())!!
						2 -> payerProviderMapping.code = currentCell.toString()
						else -> {}
					}
					cellIdx++
				}
				payerProviderMappingList.add(payerProviderMapping)
			}
			workbook.close()
			payerProviderMappingList
		} catch (e: IOException) {
			throw RuntimeException("fail to parse Excel file: " + e.message)
		}
	}

}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegionResponse(
	@JsonProperty("data")
	val data: Region,

	@JsonProperty("success")
	val success: Boolean,

	@JsonProperty("msg")
	val msg: String?
)