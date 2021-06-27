package com.example.librarykaconsumer.integration.repository

import com.example.data.LibraryEvent
import com.example.repository.LibraryEventJdbcDAO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
/**
 * Helper class, for tests
 */
class LibraryEventJdbcExtendedForTestDAO : LibraryEventJdbcDAO() {

    @Transactional
    override fun create(entity: LibraryEvent) {
        if (entity.book != null) bookJdbc.create(entity.book!!.copy(book2LibraryEvent = entity.id))
        error("For test purposes")
    }
}