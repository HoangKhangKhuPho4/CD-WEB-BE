package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.UserInteractionDto;
import com.cdweb.be.service.UserInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserInteractionController {

  private final UserInteractionService interactionService;

  @PostMapping("/track")
  public ResponseEntity<ApiResponse<UserInteractionDto.Response>> trackAction(
      @RequestBody UserInteractionDto.Request request) {
    UserInteractionDto.Response response = interactionService.trackInteraction(request);
    return ResponseEntity.ok(ApiResponse.success("Interaction tracked successfully", response));
  }
}
