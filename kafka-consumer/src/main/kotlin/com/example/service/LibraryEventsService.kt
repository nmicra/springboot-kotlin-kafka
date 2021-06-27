package com.example.service

import com.example.data.LibraryEvent
import com.example.data.LibraryEventType
import com.example.repository.LibraryEventJdbcDAO
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.RecoverableDataAccessException
import org.springframework.data.repository.CrudRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import org.springframework.util.concurrent.ListenableFutureCallback

@Service
class LibraryEventsService {

    val log: Logger = LoggerFactory.getLogger(LibraryEventsService::class.java)

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<Int, String>

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Qualifier("libraryEventJdbcDAO")
    @Autowired
    lateinit var libraryEventJdbc : LibraryEventJdbcDAO


    @Throws(JsonProcessingException::class)
    fun processLibraryEvent(consumerRecord: ConsumerRecord<Int, String>) {
        val libraryEvent = objectMapper.readValue(consumerRecord.value(), LibraryEvent::class.java)
        log.info("libraryEvent : $libraryEvent ")

        if ( libraryEvent.id == null ) { // && libraryEvent.libraryEventId != null
            throw RecoverableDataAccessException("Temporary Network Issue")
        }
        when (libraryEvent.libraryEventType) {
            LibraryEventType.NEW -> libraryEventJdbc.create(libraryEvent) //save(libraryEvent)
            LibraryEventType.UPDATE -> {
                //validate the libraryevent
                validate(libraryEvent)
                libraryEventJdbc.update(libraryEvent,libraryEvent.id)
//                save(libraryEvent)
            }
            else -> log.info("Invalid Library Event Type")
        }
    }

    private fun validate(libraryEvent: LibraryEvent) {
        requireNotNull(libraryEvent.id) { "Library Event Id is missing" }
        val libraryEventOptional = libraryEventJdbc.get(libraryEvent.id) ?: error("Not a valid library Event")
        log.info("Validation is successful for the library Event Id : ${libraryEventOptional.id} ")
    }

    /*private fun save(libraryEventJPA: LibraryEventJPA) {
        libraryEventJPA.book!!.libraryEventJPA = libraryEventJPA
        libraryEventsRepository.save(libraryEventJPA)
        log.info("Successfully Persisted the library Event $libraryEventJPA ")
    }*/

    fun handleRecovery(record: ConsumerRecord<Int?, String?>) {
        val key = record.key()
        val message = record.value()
        val listenableFuture = kafkaTemplate.sendDefault(
            key!!, message
        )
        listenableFuture.addCallback(object : ListenableFutureCallback<SendResult<Int?, String?>?> {
            override fun onFailure(ex: Throwable) {
                log.error("Failed to send [key=$key] [message=$message]", ex)
            }

            override fun onSuccess(result: SendResult<Int?, String?>?) {
                log.info("Message has been sent [key=$key] [message=$message]")
            }
        })
    }
}

interface LibraryEventsRepository : CrudRepository<LibraryEventJPA, Int>