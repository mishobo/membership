package net.lctafrica.membership.api.util

object ResultFactory {
    fun <T> getSuccessResult(data: T): Result<T> {
        return Result(success = true, data = data)
    }

    fun <T> getSuccessResult(data: T, msg: String?): Result<T> {
        return Result(success = true, data = data, msg = msg)
    }

    fun <T> getSuccessResult(msg: String?): Result<T> {
        return Result(success = true, msg = msg)
    }

    fun <T> getFailResult(msg: String?): Result<T> {
        return Result(success = false, msg = msg)
    }

    fun <T> getFailResult(data: T, msg: String?): Result<T> {
        return Result(success = false, data = data, msg = msg)
    }

}
