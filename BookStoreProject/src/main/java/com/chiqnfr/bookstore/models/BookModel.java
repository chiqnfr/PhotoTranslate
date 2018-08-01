/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chiqnfr.bookstore.models;

import com.datastax.driver.core.DataType;
import java.util.UUID;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 *
 * @author chiqnfr
 */
@Table("book")
public class BookModel{
    
    @PrimaryKey
    @CassandraType(type = DataType.Name.UUID)
    private UUID id;
    private String name;
    private String author;
    private String kind;
    private String description;
    private int quantity;

    public BookModel() {
        id = UUID.randomUUID();
    }

    public BookModel(UUID id, String name, String author, String kind, String description, int quantity) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.kind = kind;
        this.description = description;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
       
}
