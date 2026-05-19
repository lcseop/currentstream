package com.mjc813.currentstreambackend.models.mapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mapp")
public class MappingRestController {
    @Autowired
    private MappingService mappingService;
}
