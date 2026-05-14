package com.mjc813.currentstreambackend.models.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ben/user")
public class UsersRestController {
    @Autowired
    private UsersService usersService;
}
