package com.example.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.stream.Collectors

@ControllerAdvice
class LibraryEventControllerAdvice {
    val log: Logger = LoggerFactory.getLogger(LibraryEventControllerAdvice::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleRequestBody(ex: MethodArgumentNotValidException): ResponseEntity<*>? {
        val errorList = ex.bindingResult.fieldErrors
        val errorMessage = errorList.stream()
            .map { fieldError: FieldError -> fieldError.field + " - " + fieldError.defaultMessage }
            .sorted()
            .collect(Collectors.joining(", "))
        log.info("errorMessage : $errorMessage ")
        return ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<*>? {
        log.error("Bad arguments",ex)
        return ResponseEntity("Bad arguments >> $ex.message", HttpStatus.BAD_REQUEST)
    }
}