package com.example.controller

import com.example.data.LibraryEvent
import com.example.data.LibraryEventType
import com.example.producer.LibraryEventProducer
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ExecutionException

@RestController
class LibraryEventsController {

    @Autowired
    lateinit var libraryEventProducer: LibraryEventProducer



    @PostMapping("/v1/libraryevent")
    @Throws(JsonProcessingException::class, ExecutionException::class, InterruptedException::class)
    fun postLibraryEvent(@RequestBody libraryEvent: LibraryEvent): ResponseEntity<LibraryEvent> {
        val newLibraryEvent = libraryEvent.copy(libraryEventType = LibraryEventType.NEW)
        require(newLibraryEvent.book.bookId > 100) { "bookId [$newLibraryEvent.book.bookId] must be grater than 100" }
        libraryEventProducer.sendLibraryEventAsync(newLibraryEvent)
        return ResponseEntity.status(HttpStatus.CREATED).body(newLibraryEvent)
    }

    //PUT
    @PutMapping("/v1/libraryevent")
    @Throws(JsonProcessingException::class, ExecutionException::class, InterruptedException::class)
    fun putLibraryEvent(@RequestBody libraryEvent: LibraryEvent): ResponseEntity<*>? {
        if (libraryEvent.libraryEventType != LibraryEventType.UPDATE) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please pass the LibraryEventId")

        libraryEventProducer.sendLibraryEventAsync(libraryEvent)
        return ResponseEntity.status(HttpStatus.OK).body<Any>(libraryEvent)
    /*libraryEvent.libraryEventId?.let {
            libraryEventProducer.sendLibraryEventAsync(libraryEvent.copy(libraryEventType = LibraryEventType.UPDATE))
            return ResponseEntity.status(HttpStatus.OK).body<Any>(libraryEvent.copy(libraryEventType = LibraryEventType.UPDATE))
        } ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please pass the LibraryEventId")*/

    }
}