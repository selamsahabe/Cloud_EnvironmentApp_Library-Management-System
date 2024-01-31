package se.iths.librarysystem.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.librarysystem.dto.Book;
import se.iths.librarysystem.dto.Isbn;
import se.iths.librarysystem.dto.Task;
import se.iths.librarysystem.entity.BookEntity;
import se.iths.librarysystem.entity.TaskEntity;
import se.iths.librarysystem.entity.UserEntity;
import se.iths.librarysystem.exceptions.IdNotFoundException;
import se.iths.librarysystem.queue.QueueHandler;
import se.iths.librarysystem.service.BookService;
import se.iths.librarysystem.service.TaskService;
import se.iths.librarysystem.service.UserService;
import se.iths.librarysystem.validatorservice.BookValidator;
import se.iths.librarysystem.validatorservice.UserValidator;

import java.util.List;

@RestController
@RequestMapping("api/users")
public class UserBookController {

    private final UserService userService;
    private final UserValidator userValidator;
    private final BookValidator bookValidator;
    private final BookService bookService;
    private final TaskService taskService;
    private final QueueHandler queueHandler;

    public UserBookController(UserService userService, UserValidator userValidator, BookValidator bookValidator,
                              BookService bookService, TaskService taskService, QueueHandler queueHandler) {
        this.userService = userService;
        this.userValidator = userValidator;
        this.bookValidator = bookValidator;
        this.bookService = bookService;
        this.taskService = taskService;
        this.queueHandler = queueHandler;
    }

    @GetMapping("{id}/books")
    public ResponseEntity<List<Book>> getAUsersBooks(@PathVariable Long id) {
        List<Book> books = userService.getUserBooks(id);
        return new ResponseEntity<>(books, HttpStatus.OK);
    }

    @DeleteMapping("{userId}/books/{bookId}")
    public ResponseEntity<Void> removeBookUserConnection(@PathVariable Long userId, @PathVariable Long bookId) {
        UserEntity user = userService.findUserEntityById(userId).orElseThrow(() -> new IdNotFoundException("user", userId));
        BookEntity book = bookService.findById(bookId).orElseThrow(() -> new IdNotFoundException("book", bookId));
        bookValidator.hasUser(book, user);
        book.removeBorrower();
        bookService.updateBook(book);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("{id}/books")
    public ResponseEntity<Task> addBookToUser(@PathVariable Long id, @RequestBody Isbn isbn) {
        userValidator.validId(id);
        userValidator.idExists(id);
        bookValidator.isbnExists(isbn);

        TaskEntity loanTask = new TaskEntity(isbn.getIsbn(), id);
        TaskEntity savedTask = taskService.createTask(loanTask);
        Task task = queueHandler.sendToQueue(savedTask);

        return new ResponseEntity<>(task, HttpStatus.ACCEPTED);
    }

}
