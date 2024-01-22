package com.example.ContactDemo.controller;

import com.example.ContactDemo.dao.ContactRepository;
import com.example.ContactDemo.dao.UserRepository;
import com.example.ContactDemo.models.Contact;
import com.example.ContactDemo.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
public class SearchController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContactRepository contactRepository;
    @GetMapping("/search/{query}")
    @PreAuthorize("hasAuthority('Role_USER')")
    public ResponseEntity<?> search(@PathVariable("query")String query, Principal principal) {
        System.out.println("Enter!");
        User user=userRepository.getUserByUserName(principal.getName());
        System.out.println(query);
        List<Contact> contacts=this.contactRepository.findByNameContainingAndUser(query,user);
        return ResponseEntity.ok(contacts);
    }

}
