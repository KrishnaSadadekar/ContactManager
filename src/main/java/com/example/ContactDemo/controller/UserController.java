package com.example.ContactDemo.controller;

import com.example.ContactDemo.Helper.Message;
import com.example.ContactDemo.dao.ContactRepository;
import com.example.ContactDemo.dao.UserRepository;
import com.example.ContactDemo.models.Contact;
import com.example.ContactDemo.models.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    //User Dashboard
    @GetMapping("/index")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String Dashboardk(Model model, Principal principal) {
        String name = principal.getName();
        User user = userRepository.getUserByUserName(name);
        System.out.println("->" + user.getName() + "->" + user.getImageUrl());
        model.addAttribute("user", user);
        model.addAttribute("name", name);
        return "normal/user_dashboard";
    }


    //open add form controller --------->
    @GetMapping("/add-contact")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String openAddContactForm(Model model, Principal principal) {
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        String name = principal.getName();
        model.addAttribute("name", name);
        return "normal/add_contact";
    }


    @PostMapping("/processcontact")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String addContact(@ModelAttribute Contact contact,
                             @RequestParam("profileImage") MultipartFile file
            , Principal principal, HttpSession session) {
        try {
            String name = principal.getName();
            User user = userRepository.getUserByUserName(name);
            //Processing and Uploading file
            if (file.isEmpty()) {
                contact.setImage("defaultProfile.jpg");
                System.out.println("File is Empty!");
            } else {
                contact.setImage(file.getOriginalFilename());

                File savefile = new ClassPathResource("static/images").getFile();
                System.out.println(savefile.getAbsolutePath());
                Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File is uploaded!");
            }


            user.getContact().add(contact);
            contact.setUser(user);


            this.userRepository.save(user);

            System.out.println("Added to database!");
            session.setAttribute("message", new Message("One more contact is added!", "success"));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            session.setAttribute("message", new Message("Failed to add!", "failure"));
            e.printStackTrace();
        }


        return "normal/add_contact";
    }


    //Show contact handler
//    per page 5 contacts[n]
    //current page 0[page]
    @GetMapping("/contact-list/{page}")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
        model.addAttribute("title", "ContactList");

        String username = principal.getName();
        System.out.println("Name->" + username);
        User user = new User();
        user = userRepository.getUserByUserName(username);

        Pageable pageable = PageRequest.of(page, 5);
        Page<Contact> contacts = (Page<Contact>) contactRepository.findContactsByUser(user.getId(), pageable);
        model.addAttribute("contact", contacts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPage", contacts.getTotalPages());
        String name = principal.getName();
        System.out.println("Title name ->" + name);
        model.addAttribute("name", name);
        return "normal/contactList";
    }


    //Showing particular Contact Details

    @GetMapping("/{cId}/contact")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {

        Optional<Contact> contactOptional = this.contactRepository.findById(cId);

        Contact contact = contactOptional.get();

        String userName = principal.getName();
        User user = this.userRepository.getUserByUserName(userName);
        System.out.println("cId" + cId);
        System.out.println("->" + contact.getName() + "->" + contact.getImage());
        model.addAttribute("title", "ContactDetail");

        model.addAttribute("name", userName);
        model.addAttribute("contact", contact);
        return "normal/contact_detail";

    }

    @GetMapping("/delete/{cId}")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session) throws IOException {

        Optional<Contact> contactOptional = contactRepository.findById(cId);
        Contact contact = contactOptional.get();
        this.contactRepository.delete(contact);

        try {

            File savefile = new ClassPathResource("static/images").getFile();
            Path path = Paths.get(savefile.getAbsolutePath() + File.separator + contact.getImage());
            Files.delete(path);
            session.setAttribute("message", new Message("contact deleted successfully", "success"));

        } catch (Exception e) {
            System.out.println("Error:" + e);
            e.printStackTrace();
        }


        return "redirect:/user/contact-list/0";
    }

    @GetMapping("/update/{cId}")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String updateContact(@PathVariable("cId") Integer cId, Model model) {
        Optional<Contact> contactOptional = contactRepository.findById(cId);
        Contact contact = contactOptional.get();
        model.addAttribute("contact", contact);
        model.addAttribute("title", "UpdateContact");
        return "normal/update";
    }

    //    Updating contact details
    @PostMapping("/process-contact/{cId}")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String updateHandler(@RequestParam("profileImage") MultipartFile file,
                                @PathVariable("cId") Integer cId, @ModelAttribute Contact contact
            , Principal principal, HttpSession session) {
        System.out.println("The cId" + cId);
//        Contact details
        Optional<Contact> old = this.contactRepository.findById(cId);
        Contact oldContact = old.get();
        System.out.println("Old->"+oldContact.getImage());
        contact.setcId(cId);
        contact.setUser(oldContact.getUser());
        try {
            if (!file.isEmpty()) {
//delete first old file-------------------

                if (oldContact.getImage() != null && (!oldContact.getImage().equals("defaultProfile.jpg"))){
                File savefile = new ClassPathResource("static/images").getFile();
                System.out.println();
                Path path = Paths.get(savefile.getAbsolutePath() + File.separator + oldContact.getImage());
                Files.delete(path);
                System.out.println("deleted!" + savefile.getName() + "----");
                }
//upload file----------------------------------------

                contact.setImage(file.getOriginalFilename());
                System.out.println("File Name: " + file.getOriginalFilename());

                File savefile1 = new ClassPathResource("static/images").getFile();
                Path path1 = Paths.get(savefile1.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path1, StandardCopyOption.REPLACE_EXISTING);
                this.contactRepository.save(contact);
                session.setAttribute("message", new Message("contact updated successfully", "success"));
            } else {
                contact.setImage(oldContact.getImage());
                this.contactRepository.save(contact);
                session.setAttribute("message", new Message("contact updated successfully", "success"));

            }
        } catch (Exception e) {
            session.setAttribute("message", new Message("Try Again!", "failure"));

            e.printStackTrace();
        }

        return "redirect:/user/update/" + cId;
    }

    //Profile Picture!
    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String myProfile(Model model, Principal principal) {
        String name = principal.getName();
        User user = userRepository.getUserByUserName(name);
        model.addAttribute("user", user);
        model.addAttribute("name", name);
        model.addAttribute("title", "Profile");
        return "normal/profile";
    }

    @GetMapping("/profile-update/{id}")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String profileUpdate(@PathVariable("id") Integer id, Model model, Principal principal) {
        String name = principal.getName();
        User user = userRepository.getUserByUserName(name);
        model.addAttribute("user", user);

        model.addAttribute("title", "Update_profile");
        return "normal/updateprofile";
    }

    @PostMapping("/process-user/{id}")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String updateUser(@Valid @ModelAttribute("user") User user, BindingResult bResult,
                             @RequestParam("profileImage") MultipartFile file,
                             Model model, HttpSession session, Principal principal) {


        String name = principal.getName();
        User olduser = userRepository.getUserByUserName(name);

        //Set rest Values
        try {
            if (!file.isEmpty()) {
//delete first old file-------------------
                if (olduser.getImageUrl() != null && (!olduser.getImageUrl().equals("defaultProfile.jpg"))) {
                    File savefile = new ClassPathResource("static/images").getFile();
                    Path path = Paths.get(savefile.getAbsolutePath() + File.separator + olduser.getImageUrl());
                    Files.delete(path);
                    System.out.println("deleted!");
                }
//upload file----------------------------------------

                user.setImageUrl(file.getOriginalFilename());
                System.out.println("File Name: " + file.getOriginalFilename());

                File savefile1 = new ClassPathResource("static/images").getFile();
                Path path1 = Paths.get(savefile1.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path1, StandardCopyOption.REPLACE_EXISTING);

                session.setAttribute("message", new Message("contact updated successfully", "success"));
            } else {
                user.setImageUrl(olduser.getImageUrl());

                session.setAttribute("message", new Message("contact updated successfully", "success"));

            }
        } catch (Exception e) {
            session.setAttribute("message", new Message("Try Again!", "failure"));

            e.printStackTrace();
        }


//----------------------------------------------------
        user.setPassword(olduser.getPassword());
        user.setEmail(olduser.getEmail());
        user.setId(olduser.getId());
        user.setRole("Role_USER");
        user.setEnabled(true);
        user.setContact(olduser.getContact());

        if (bResult.hasErrors()) {
            System.out.println("Error->" + bResult.toString());
            model.addAttribute("user", user);
            return "normal/updateprofile";
        }


        userRepository.save(user);
        return "normal/updateprofile";
    }
//Setting

    @GetMapping("/setting")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String setting(Model model, Principal principal) {
        String name = principal.getName();
        User user = userRepository.getUserByUserName(name);
        model.addAttribute("user", user);
        model.addAttribute("name", name);
        model.addAttribute("title", "setting");
        return "normal/setting";
    }

    @PostMapping("/process-setting")
    @PreAuthorize("hasAuthority('Role_USER')")
    public String changepassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 Principal principal,HttpSession session)
    {
        User user=userRepository.getUserByUserName(principal.getName());
        System.out.println("Encoded Password: "+user.getPassword());
        System.out.println("Current Password"+currentPassword+" New Password: "+newPassword);

        if(passwordEncoder.matches(currentPassword,user.getPassword()))
        {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            session.setAttribute("message", new Message("password updated successfully", "success"));
        }else
        {
            session.setAttribute("message", new Message("Wrong Pasword!", "failure"));
        }
        return "normal/setting";
    }
}
