package com.nll.store.api

sealed class ApiException(
    message: String? = null,
    throwable: Throwable? = null
) : RuntimeException(message, throwable) {
    class TimeoutException(
        throwable: Throwable
    ) : ApiException(message = throwable.message, throwable = throwable)

    class ServerException(
        throwable: Throwable? = null,
    ) : ApiException(message = throwable?.message, throwable = throwable)
    class UnknownHostException(
        throwable: Throwable? = null
    ) : ApiException(message = throwable?.message, throwable = throwable)

    class GenericException(
        throwable: Throwable? = null,
    ) : ApiException(throwable?.message, throwable)
    sealed class HttpException(
        statusCode: Int,
        throwable: Throwable? = null,
    ) : ApiException(throwable?.message, throwable) {
        class AuthenticationException(
            statusCode: Int,
            throwable: Throwable? = null
        ) : HttpException(statusCode, throwable)

        class PermissionException(
            statusCode: Int,
            throwable: Throwable? = null
        ) : HttpException(statusCode, throwable)


        class UnknownException(
            statusCode: Int,
            throwable: Throwable? = null
        ) : HttpException(statusCode, throwable)

    }


}
