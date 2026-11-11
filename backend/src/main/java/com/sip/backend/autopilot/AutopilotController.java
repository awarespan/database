package com.sip.backend.autopilot;

import com.sip.backend.dto.AutopilotPolicyDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/autopilot")
public class AutopilotController {

    private final AutopilotService autopilotService;

    public AutopilotController(AutopilotService autopilotService) {
        this.autopilotService = autopilotService;
    }

    @GetMapping("/policy")
    public ResponseEntity<AutopilotPolicyDto> getPolicy(@RequestParam String nodeId) {
        return ResponseEntity.ok(autopilotService.getPolicy(nodeId));
    }

    @PutMapping("/policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AutopilotPolicyDto> updatePolicy(
            @RequestParam String nodeId,
            @RequestBody AutopilotPolicyDto dto) {
        return ResponseEntity.ok(autopilotService.updatePolicy(nodeId, dto));
    }
}