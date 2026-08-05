package com.semd.backend.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final Map<String, ResponseEntity<?>> cache = new ConcurrentHashMap<>();

    public boolean isDuplicate(String key) {
        return key != null && cache.containsKey(key);
    }

    public void save(String key, ResponseEntity<?> response) {
        if (key != null && response != null) {
            cache.put(key, response);
        }
    }

    public ResponseEntity<?> getResponse(String key) {
        return cache.get(key);
    }
}
