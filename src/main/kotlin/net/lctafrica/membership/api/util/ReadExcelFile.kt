package net.lctafrica.membership.api.util

import net.lctafrica.membership.api.domain.BeneficiaryType
import net.lctafrica.membership.api.domain.Gender
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.*

@Component
class ReadExcelFile {

	val fileType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	val headers =
		arrayListOf("FLAG", "NAME", "DOB", "GENDER", "TITLE", "MEMBER NUMBER", "EMAIL", "PHONE")

	fun isExcelFormat(file: MultipartFile): Boolean {
		return file.contentType.equals(fileType)
	}

	fun read(input: InputStream): MutableMap<String, MutableList<MemberInput>> {
		val map = mutableMapOf<String, MutableList<MemberInput>>()
		val workBook = XSSFWorkbook(input)
		//val shIterator = workBook.spliterator()
		val formatter = DataFormatter()
		workBook.map {
			val cat = it.sheetName
			val members = mutableListOf<MemberInput>()
			System.out.println(it.rowIterator().hasNext())

			it.rowIterator().forEach { r ->
				if (r.rowNum != 0) {

					if (r.getCell(1) !== null && r.getCell(2).dateCellValue !== null && r.getCell(3) !== null
						&& r.getCell(4) !== null && r.getCell(5) !== null
					) {
						val member = MemberInput(
							tracker = formatter.formatCellValue(r.getCell(0)),
							name = r.getCell(1).stringCellValue,
							dob = r.getCell(2).dateCellValue,
							gender = Gender.valueOf(
								r.getCell(3).stringCellValue.uppercase()
									.trim()
							),
							title = BeneficiaryType.valueOf(
								r.getCell(4).stringCellValue
									.uppercase().trim()
							),
							memberNumber = formatter.formatCellValue(r.getCell(5)),
							email = r.getCell(6, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK)
								?.stringCellValue,
							phone = formatter.formatCellValue(
								r.getCell(
									7, Row
										.MissingCellPolicy.RETURN_NULL_AND_BLANK
								)
							),
							nhif = formatter.formatCellValue(
								r.getCell(
									8, Row
										.MissingCellPolicy.RETURN_NULL_AND_BLANK
								)
							),
							id = formatter.formatCellValue(
								r.getCell(
									9, Row.MissingCellPolicy
										.RETURN_NULL_AND_BLANK
								)
							),

							)
						members.add(member)
					}

				}
			}
			map[cat.uppercase().trim()] = members
		}
		return map
	}

	fun getSheets(input: InputStream): MutableList<String> {
		val sheetNames = mutableListOf<String>()
		val workBook = OPCPackage.open(input)
		val reader = XSSFReader(workBook)
		val sheets = reader.sheetsData
		println(sheets.toString())
		if (sheets is XSSFReader.SheetIterator) {
			val sheetIterator: XSSFReader.SheetIterator = sheets
			while (sheetIterator.hasNext()) {
//                val next = sheetIterator.next()
				println("****************")
				println(sheetIterator.sheetName)
				sheetNames.add(sheetIterator.sheetName)
//                val sheet = reader.getSheet(sheetIterator.sheetName)
			}
		}
		return sheetNames
	}


}

data class MemberInput(
	val tracker: String,
	val name: String,
	val dob: Date,
	val gender: Gender,
	val title: BeneficiaryType,
	val memberNumber: String,
	var email: String?,
	var phone: String?,
	var nhif: String?,
	var id: String?,
	var jicEntityId: Int? = null,
	var apaEntityId: Int? = null

)