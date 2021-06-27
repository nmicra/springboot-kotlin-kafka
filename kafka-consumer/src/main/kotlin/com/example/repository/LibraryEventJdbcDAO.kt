package com.example.repository

import com.example.data.Book
import com.example.data.DAO
import com.example.data.LibraryEvent
import com.example.data.LibraryEventType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import javax.annotation.PostConstruct

@Service
class LibraryEventJdbcDAO : DAO<LibraryEvent> {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var bookJdbc : BookJdbcDAO

    @PostConstruct
    fun createTable(){
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_library_event(id VARCHAR(50), libraryEventType VARCHAR(50), PRIMARY KEY(id))")
    }

    val rowMapper : RowMapper<LibraryEvent> = RowMapper<LibraryEvent> { resultSet : ResultSet, _:Int -> LibraryEvent(
        LibraryEventType.valueOf(resultSet.getString("libraryEventType")),
        resultSet.getString("id")) }

    override fun list(): List<LibraryEvent>? {
        return jdbcTemplate.query("SELECT id, libraryEventType FROM t_library_event", rowMapper)
    }

    @Transactional
    override fun create(entity: LibraryEvent) {
        if (entity.book != null) bookJdbc.create(entity.book.copy(book2LibraryEvent = entity.id))
        jdbcTemplate.execute("INSERT INTO t_library_event(id, libraryEventType) values('${entity.id}','${entity.libraryEventType}')")
    }

    override fun get(id: String): LibraryEvent? {
        val queryResult = jdbcTemplate.query("SELECT * FROM t_library_event WHERE id = '$id'", rowMapper)
        return when (queryResult.size){
            0 -> null
            1 -> {
                val findBookByLibraryTypeId = bookJdbc.findBookByLibraryTypeId(id)
                queryResult[0].copy(book = findBookByLibraryTypeId[0])
            }
            else -> error("found too many records in t_library_event with the same id='$id'")
        }
    }

    @Transactional
    override fun update(entity: LibraryEvent, id: String) {
        if (entity.book != null) bookJdbc.update(entity.book.copy(book2LibraryEvent = entity.id),entity.book.id)
        jdbcTemplate.execute("UPDATE t_library_event SET libraryEventType='${entity.libraryEventType}' where id=$id")
    }

    override fun delete(id: String) {
        jdbcTemplate.execute("DELETE FROM t_library_event where id='$id'")
    }
}