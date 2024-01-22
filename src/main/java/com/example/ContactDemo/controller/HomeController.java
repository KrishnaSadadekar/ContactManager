package com.example.ContactDemo.controller;

import com.example.ContactDemo.Helper.Message;
import com.example.ContactDemo.dao.UserRepository;
import com.example.ContactDemo.models.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class HomeController {

    @Autowired
    UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("title", "HomePage");
        return "home";
    }


    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "AboutUs");
        return "about";
    }

    @GetMapping("/signin")
    public String signIn() {
        return "signin";
    }

    @GetMapping("/signinU")
    public String userSign() {
        return "home";
    }


    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("title", "SignUp");
        model.addAttribute("user", new User());
        return "signup";
    }

    // handler for form------------------------------

    @PostMapping("/doregister")
    public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult bResult, @RequestParam(value = "agreement", defaultValue = "false") boolean agreement, Model model, HttpSession session) {

        try {

            if (!agreement) {
                System.out.println("you have not agreed terms and conditions!");
                throw new Exception("you have not agreed terms and conditions!");

            }

            if (bResult.hasErrors()) {
                System.out.println("Error->" + bResult.toString());
                model.addAttribute("user", user);
                return "signup";
            }
            user.setRole("Role_USER");
            user.setEnabled(true);
            user.setImageUrl("defaultProfile.jpg");
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            User result = this.userRepository.save(user);

            model.addAttribute("user", new User());
            session.setAttribute("message", new Message("Registration successfully", "alert-success"));
            return "signup";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("user", user);
            session.setAttribute("message", new Message("Something went wrong" + e.getMessage(), "alert-danger"));
            return "signup";
        }


    }

}
