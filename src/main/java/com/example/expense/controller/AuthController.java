package com.example.expense.controller;

import com.example.expense.model.User;
import com.example.expense.repository.UserRepository;
import com.example.expense.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder;

    public AuthController(UserRepository repo, BCryptPasswordEncoder encoder){
        this.repo=repo;
        this.encoder=encoder;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user){
        user.setPassword(encoder.encode(user.getPassword()));
        return repo.save(user);
    }

    @PostMapping("/login")
    public String login(@RequestBody User user){
        User dbUser = repo.findByUsername(user.getUsername());
        if(dbUser!=null && encoder.matches(user.getPassword(), dbUser.getPassword())){
            return JwtUtil.generateToken(user.getUsername());
        }
        return "Invalid";
    }
}
