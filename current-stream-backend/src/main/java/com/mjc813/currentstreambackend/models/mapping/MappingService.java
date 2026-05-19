package com.mjc813.currentstreambackend.models.mapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MappingService {
    @Autowired
    private MappingRepository mappingRepository;
}
