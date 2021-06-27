package com.example.data


data class Book(val bookId : Int, val bookName : String, val bookAuthor : String){
    init {
        require(bookId >= 0) { "bookId [$bookId] must be grater than 0" }
    }

}
enum class LibraryEventType { NEW, UPDATE, NONE }
data class LibraryEvent(val libraryEventId: Int, val libraryEventType : LibraryEventType = LibraryEventType.NONE, val book : Book)