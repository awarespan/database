package com.sip.backend.controller;

import com.sip.backend.entity.Command;
import com.sip.backend.service.CommandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/commands")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/dispatch/{nodeId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<Command> dispatchCommand(
            @PathVariable String nodeId,
            @RequestParam String type,
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestParam(defaultValue = "0") int priority,
            @RequestParam(defaultValue = "300") int ttlSeconds) {
        try {
            Command.CommandType commandType = Command.CommandType.valueOf(type.toUpperCase());
            Command cmd = commandService.dispatchCommand(nodeId, commandType, payload, priority, ttlSeconds);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/node/{nodeId}")
    public ResponseEntity<List<Command>> getNodeCommands(@PathVariable String nodeId) {
        return ResponseEntity.ok(commandService.findByNodeId(nodeId));
    }

    @GetMapping("/pending/{nodeId}")
    public ResponseEntity<List<Command>> getPendingCommands(@PathVariable String nodeId) {
        return ResponseEntity.ok(commandService.getPendingForNode(nodeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Command> getCommand(@PathVariable String id) {
        return commandService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledge(@PathVariable String id) {
        commandService.acknowledgeCommand(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> fail(@PathVariable String id, @RequestParam String reason) {
        commandService.failCommand(id, reason);
        return ResponseEntity.ok().build();
    }
}