package net.lctafrica.membership.api.web

import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

abstract class AbstractController {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): Result<MutableMap<String, String?>> {
        val errors = mutableMapOf<String, String?>()
        ex.bindingResult.allErrors.forEach() { err ->
            val fieldName = (err as FieldError).field
            val message = err.defaultMessage
            errors[fieldName] = message
        }
        return ResultFactory.getFailResult(msg="Validation of fields failed", data = errors)
    }
}