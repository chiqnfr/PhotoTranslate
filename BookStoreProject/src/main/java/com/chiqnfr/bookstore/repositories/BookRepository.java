/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chiqnfr.bookstore.repositories;

import com.chiqnfr.bookstore.models.BookModel;
import java.util.UUID;
import org.springframework.data.cassandra.repository.CassandraRepository;

/**
 *
 * @author chiqnfr
 */
public interface BookRepository extends CassandraRepository<BookModel, UUID> {
    
}
