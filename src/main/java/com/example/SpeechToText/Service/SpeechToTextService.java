package com.example.SpeechToText.Service;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



@Service
public class SpeechToTextService {


    public void initializeClient() throws IOException {
        SpeechSettings speechSettings = SpeechSettings.newBuilder()
                .setCredentialsProvider(() -> GoogleCredentials.fromStream(new FileInputStream("/Users/kimseungzzang/Downloads/zoom-speech-to-text-444609-309faf0332bb.json")))
                .build();
    }

    public String convertSpeechToText(String audioFilePath) {
        try {
            // Google Cloud SpeechClient 생성
            try (SpeechClient speechClient = SpeechClient.create()) {

                File inputFile = new File(audioFilePath);
                String parentDir = inputFile.getParent(); // 입력 파일의 디렉토리
                String fileName = inputFile.getName(); // 입력 파일의 이름

                // 파일 이름에서 확장자 제거 및 'new' 추가
                String newFileName = fileName.replaceFirst("\\.[^.]+$", "") + "_new.wav";

                // 최종 경로 생성
                String outputFilePath = new File(parentDir, newFileName).getAbsolutePath();

                String command = String.format(
                        "ffmpeg -i %s -ar %d -ac 1 %s",
                        audioFilePath,
                        16000,
                        outputFilePath
                );


                Process process = Runtime.getRuntime().exec(command);
                process.waitFor();

                String gcsUri = uploadToGcs("zoom-speech-to-text-444609", "zoom_speech_to_text", outputFilePath);

                System.out.println(gcsUri);
                // 음성 인식 설정
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16) // FLAC 파일로 인코딩 설정
                        .setSampleRateHertz(16000)
                        .setLanguageCode("ko-KR")
                        .addAlternativeLanguageCodes("en-US") // 추가 언어 설정
                        .build();

                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setUri(gcsUri)
                        .build();

                // LongRunningRecognize 요청
                LongRunningRecognizeRequest request = LongRunningRecognizeRequest.newBuilder()
                        .setConfig(config)
                        .setAudio(audio)
                        .build();

                OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
                        speechClient.longRunningRecognizeAsync(request);

                // 작업 완료를 기다림
                while (!response.isDone()) {
                    System.out.println("Waiting for response...");
                    Thread.sleep(10000); // 10초 대기
                }

                // 결과를 가져옴
                LongRunningRecognizeResponse longRunningResponse = response.get();

                // 결과에서 텍스트 추출 및 기타 메타데이터 출력
                StringBuilder transcript = new StringBuilder();
                for (SpeechRecognitionResult result : longRunningResponse.getResultsList()) {
                    transcript.append(result.getAlternatives(0).getTranscript()).append("\n");
                }

                return transcript.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing audio file: " + e.getMessage();
        }
    }

    public static String uploadToGcs(String projectId, String bucketName, String filePath) throws Exception {
        // GCS 클라이언트 생성
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        // 파일 정보
        Path path = Paths.get(filePath);
        String blobName = path.getFileName().toString();

        // BlobId와 BlobInfo 설정
        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        // 파일 업로드
        storage.create(blobInfo, Files.readAllBytes(path));

        // GCS URI 반환
        return "gs://" + bucketName + "/" + blobName;
    }
}



