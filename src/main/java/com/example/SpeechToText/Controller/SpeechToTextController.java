package com.example.SpeechToText.Controller;


import com.example.SpeechToText.Service.GptService;
import com.example.SpeechToText.Service.SpeechToTextService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Controller
public class SpeechToTextController {

    private SpeechToTextService speechToTextService;
    private GptService gptService;

    private final Path uploadPath = Paths.get("src/main/resources/uploads"); // 절대 경로로 설정

    public SpeechToTextController(SpeechToTextService speechToTextService,GptService gptService) {
        this.speechToTextService = speechToTextService;
        this.gptService=gptService;
    }


    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("audio") MultipartFile file) {
        try {
            // 업로드 경로 확인 및 폴더 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;

            Path path = uploadPath.resolve(uniqueFilename);
            Files.write(path, file.getBytes());
            return ResponseEntity.ok(path.toString()); // 로컬 파일 경로 반환
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 업로드 실패: " + e.getMessage());
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<String> convertSpeechToText(@RequestBody Map<String, String> request) {
        String audioFilePath = request.get("audioFilePath");
        String text=speechToTextService.convertSpeechToText(audioFilePath);
        String summary=gptService.getGPTSummary(text);

        return ResponseEntity.ok(summary);
    }

    @PostMapping("/processText")
    public ResponseEntity<String> processText(@RequestBody Map<String, String> request) {
        String recognizedText = request.get("recognizedText");


        System.out.println("Received recognized text: " + recognizedText);

        return ResponseEntity.ok("Text processed successfully: " + recognizedText);
    }
}
