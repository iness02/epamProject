package com.example.epamProject.controller;


import com.example.epamProject.csv.ResponseMessage;
import com.example.epamProject.dto.DoctorDTO;
import com.example.epamProject.service.DoctorService;
import com.example.epamProject.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/doctors")
public class DoctorController {
    private final DoctorService doctorService;
    private final EmailService emailService;


    @Autowired
    public DoctorController(DoctorService doctorService, EmailService emailService) {
        this.doctorService = doctorService;
        this.emailService = emailService;
    }

    @PostMapping("/import-csv")
    public ResponseEntity<ResponseMessage> uploadFile(@RequestBody MultipartFile file) {

        try {
            doctorService.save(file);
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/csv/download/doctors/")
                    .path(file.getName())
                    .toUriString();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseMessage("csv data uploaded", fileDownloadUri));
        } catch (Exception e) {
            String message = e.getMessage() + file.getOriginalFilename() + "!";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseMessage(message, ""));
        }
    }


    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/all")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))

    public ResponseEntity<List<DoctorDTO>> getAllDoctors() {
        List<DoctorDTO> doctors = doctorService.getAllDoctors();
        return new ResponseEntity<>(doctors, HttpStatus.OK);
    }

    @GetMapping("/{doctorId}")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))

    public ResponseEntity<DoctorDTO> getDoctorById(@PathVariable Long doctorId) {
        DoctorDTO doctor = doctorService.getDoctorById(doctorId);
        return new ResponseEntity<>(doctor, HttpStatus.OK);
    }

    // API to search doctors by name
    @GetMapping("/search")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))

    public ResponseEntity<List<DoctorDTO>> searchDoctors(@RequestParam("query") String query) {
        List<DoctorDTO> doctors = doctorService.searchDoctors(query);
        return new ResponseEntity<>(doctors, HttpStatus.OK);
    }

    @PostMapping("/sendEmail")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))

    public ResponseEntity<String> sendEmailToDoctor(
            @RequestParam("email") String email,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message
    ) {

        String successMessage = "Email sent successfully.";
        String errorMessage = "Failed to send email.";


        try {
            emailService.sendEmailToDoctor(email, subject, message);

            return ResponseEntity.status(HttpStatus.OK).body(successMessage);
        } catch (Exception e) {
            String detailedError = errorMessage + " Error: " + e.getMessage();
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detailedError);
        }
    }


}
