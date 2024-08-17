package dev.aniket.controllers;

import dev.aniket.consts.AppConst;
import dev.aniket.implementation.VideoServiceImpl;
import dev.aniket.model.Video;
import dev.aniket.playload.CustomMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

//video upload

@CrossOrigin(origins = "http://localhost:5173/")
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {
    private final VideoServiceImpl videoService;

    // add video

    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam("file")MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description
            )
    {
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        //Also we use UUID for id

        Video savedVideo = videoService.save(video, file);

        //video is not uploaded
        if (savedVideo == null) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomMessage
                            .builder()
                            .message("Video Not Uploaded!")
                            .success(false)
                            .build()
                    );
        }

        //video is uploaded
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(savedVideo);
    }

    //stream video

    @GetMapping("/stream/{videoId}")
    public ResponseEntity<?> stream(@PathVariable Integer videoId) {
        Video video = videoService.getById(videoId);

        String contentType = video.getContentType();
        String filePath = video.getFilePath();

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        Resource resource = new FileSystemResource(filePath);
        log.info("file path is: " + filePath);
        log.info("Print resource: " + resource);
        log.warn("Sending complete video data!");

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // get all video

    @GetMapping
    public List<Video> getAllVideos() {
        return videoService.getAll();
    }

    // stream video in chunks

    @GetMapping("/stream/range/{videoId}")
    public ResponseEntity<?> streamVideoRange(
            @PathVariable Integer videoId,
            @RequestHeader(value = "Range", required = false) String range
    )
    {
        log.info("Range is coming for the front-end: " + range);

        Video video = videoService.getById(videoId);
        Path path = Paths.get(video.getFilePath());

        log.info("video path is: " + path.toString());

        Resource resource = new FileSystemResource(path);

        String contentType = video.getContentType();

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        //file size
        Long fileSize = path.toFile().length();

        //header is not present
        if (range == null) {
            //returning the full video
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }

        //TODO calculating start and end range

        //if in header present the range
        long rangeStart, rangeEnd;
        String[] originalRange = range.replace("bytes=", "").split("-");
        rangeStart = Long.parseLong(originalRange[0]);

        //only send the 1MB data at that time
        rangeEnd = rangeStart + AppConst.CHUNK_SIZE - 1;

        //if rangeEnd is large then the file size
        if (rangeEnd >= fileSize) {
            rangeEnd = fileSize - 1;
        }

        //check the last index is exist or not
//        if (originalRange.length > 1) {
//            rangeEnd = Long.parseLong(originalRange[1]);
//        } else {
//            //if rangeEnd is not sending // bytes=10001-
//            rangeEnd = fileSize - 1;
//        }

        //if rangeEnd is large then the file lenght
//        if (rangeEnd > (fileSize - 1)) {
//            rangeEnd = fileSize - 1;
//        }

        //print the range
        log.info("server send video starting is: " + rangeStart);
        log.info("server send video Ending is: " + rangeEnd);

        InputStream inputStream;

        try {
            inputStream = Files.newInputStream(path);

            //skip the starting byte to the rangeStart
            inputStream.skip(rangeStart);

            long contentLength = rangeEnd - rangeStart + 1;

            byte[] data = new byte[(int) contentLength];
            int read = inputStream.read(data, 0, data.length);
//            data = inputStream.readAllBytes();
//            System.out.println("read(number of byte) : " + read);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileSize);

            //security-related HTTP headers
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("X-Content-Type-Options", "nosniff");

            //set content length
            headers.setContentLength(contentLength);

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(
                            new ByteArrayResource(data)
                    );

        } catch (IOException e) {
            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomMessage
                            .builder()
                            .message("ERROR: " + e.getMessage())
                            .success(false)
                            .build());
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomMessage
                            .builder()
                            .message("ERROR: " + e.getMessage())
                            .success(false)
                            .build());
        }

    }

    /*server hls playlist*/

    //master.m3u8

    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<?> serverMasterFile(@PathVariable Integer videoId) {
        return videoService.executeMasterFile(videoId);
    }

    //segments

    @GetMapping("{videoId}/{segment}.ts")
    public ResponseEntity<?> serverSegmentFile(
            @PathVariable Integer videoId,
            @PathVariable String segment
    )
    {
        return videoService.executeSegmentFile(videoId, segment);
    }

}

//PARTIAL_CONTENT => means we are sending the video in the chucks
/*
* using the Content-Range header and 206 Partial Content status code
* is a correct way to implement video streaming, where the server sends
* chunks of the video file to the client, and the client reassembles the chunks
* to play the video.
* */