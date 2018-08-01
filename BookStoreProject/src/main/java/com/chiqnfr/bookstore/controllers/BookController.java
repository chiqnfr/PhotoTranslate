/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chiqnfr.bookstore.controllers;

import com.chiqnfr.bookstore.models.BookModel;
import com.chiqnfr.bookstore.repositories.BookRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author chiqnfr
 */
@Controller
public class BookController {
    
    @Autowired
    private BookRepository bookRepository;
    
    @RequestMapping(value = {"/", "/list-book"})
    public String listBook(Model model) {
        
        model.addAttribute("listBook", bookRepository.findAll());
        return "list-book";
    }
    
    @RequestMapping("/save-book")
    public String saveBook(Model model) {
        
        model.addAttribute("book", new BookModel());
        return "save-book";
    }
    
    @RequestMapping("/saveBook")
    public String doSaveBook (@ModelAttribute("BookModel") BookModel book, Model model) {
        
        bookRepository.save(book);
        model.addAttribute("listBook", bookRepository.findAll());
        return "list-book";
    }
    
    @RequestMapping("/edit-book/{id}")
    public String editBook(@PathVariable UUID id, Model model) {
        
        Optional<BookModel> book = bookRepository.findById(id);
        if (book.isPresent()) {
            model.addAttribute("book", book.get());
        }
        return "edit-book";
    }
    
    @RequestMapping("/editBook")
    public String doEditBook(@ModelAttribute("BookModel") BookModel book, Model model) {
        
        bookRepository.save(book);
        model.addAttribute("listBook", bookRepository.findAll());
        return "list-book";
    }
  
    @RequestMapping("/deleteBook/{id}")
    public String doDeleteBook(@PathVariable UUID id, Model model) {
        
        bookRepository.deleteById(id);
        model.addAttribute("listBook", bookRepository.findAll());
        return "list-book";
    }
}
