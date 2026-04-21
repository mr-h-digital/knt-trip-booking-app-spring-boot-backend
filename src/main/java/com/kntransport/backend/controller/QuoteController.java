package com.kntransport.backend.controller;

import com.kntransport.backend.dto.QuoteAcceptRequest;
import com.kntransport.backend.dto.QuoteDto;
import com.kntransport.backend.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping("/{id}")
    public QuoteDto getQuote(@PathVariable String id) {
        return quoteService.getQuote(id);
    }

    @PostMapping("/{id}/respond")
    public QuoteDto respondToQuote(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id,
            @Valid @RequestBody QuoteAcceptRequest request) {
        return quoteService.respondToQuote(id, request);
    }
}
