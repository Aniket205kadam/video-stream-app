package dev.aniket.implementation;

import dev.aniket.dao.VideoDao;
import dev.aniket.model.Video;
import dev.aniket.service.VideoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;


@Service
@Slf4j
public class VideoServiceImpl implements VideoService {
    private final VideoDao videoDao;

    @Value("${files.video}")
    String DIR;

    @Value("${newFile.video.hls}")
    String HLS_DIR;

    @Autowired
    public VideoServiceImpl(VideoDao videoDao) {
        this.videoDao = videoDao;
    }

    @PostConstruct
    public void init() {
        File file1 = new File(DIR);
        File file2 = new File(HLS_DIR);

        //file1
        if (!file1.exists()) {
            log.info("{} folder is created!", DIR);
            System.out.println((file1.mkdir()) ? DIR + " folder is created!" : DIR + " folder is not created!");
        } else {
            log.info("{} folder is already created!", DIR);
        }

        //file2
        if (!file2.exists()) {
            log.info("{} folder is created!", HLS_DIR);
            System.out.println((file2.mkdir()) ? HLS_DIR + " folder is created!" : HLS_DIR + " folder is not created!");
        } else {
            log.info("{} folder is already created!", HLS_DIR);
        }
    }

    @Override
    public Video save(Video video, MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();
            long size = file.getSize();

            //check the content-type
//            assert contentType != null;
            if (!contentType.startsWith("video/")) {
                //image, audio, other files are not allowed
                throw new RuntimeException("Image, Audio and Other files are not allowed!");
            }

            // clean the folder and file path
            filename = StringUtils.cleanPath(filename);
            DIR = StringUtils.cleanPath(DIR);

            // folder path with filename
            Path path = Paths.get(DIR, filename);

            // for the console or debugging
            log.info("Video path is: {}", path);
            log.info("Video content-type is: {}", file.getContentType());
            log.info("Video size is: {}", size);

            // copy file to the folder
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

            //check content-type
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // video meta data
            video.setContentType(contentType);
            video.setFilePath(path.toString());

            //processing video
            String uniqueId = "";
            try {
                uniqueId = processVideo(path);
            } catch (Exception e) {
                //deleting actual video file if exception
                Files.deleteIfExists(path);
                log.error("Some reason the current file is deleted, this file path is: {}", path.toString());

                //stop the method
                throw new RuntimeException("Video Processing Failed!");
            }

            //attach the unique id given by the processVideo() method
            video.setUniqueId(uniqueId);

            // metadata save
            return videoDao.save(video);

        } catch (IOException exception) {
            log.error("Video saving failed : {}", exception.getMessage());
            return null;
        }
    }

    @Override
    public Video getById(Integer videoId) {
        return videoDao
                .findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video Not Found!"));
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoDao.findAll();
    }

    @Override
    public List<Video> getAllByIds(List<Integer> ids) {
        return List.of();
    }

    @Override
    public String processVideo(Path videoPath) {
        //the commented part use later....
//        String output360p = HLS_DIR + videoId + "/360p/";
//        String output720p = HLS_DIR + videoId + "/720p/";
//        String output1080p = HLS_DIR + videoId + "/1080p/";

        //create the dir
        try {
//            Files.createDirectories(Paths.get(output360p));
//            Files.createDirectories(Paths.get(output720p));
//            Files.createDirectories(Paths.get(output1080p));

            String uniqueId = String.valueOf(UUID.randomUUID());
            Path outputPath = Paths.get(HLS_DIR, uniqueId);

            //create dir
            Files.createDirectories(outputPath);

            //ffmpeg command
            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    videoPath,
                    outputPath,
                    outputPath
            );
            //two output path is passed one for the master file and another for the segment

//            StringBuilder ffmpegCmd = new StringBuilder();
//
//            ffmpegCmd.append("ffmpeg  -i ")
//                    .append(videoPath.toString())
//                    .append(" -c:v libx264 -c:a aac")
//                    .append(" ")
//                    .append("-map 0:v -map 0:a -s:v:0 640x360 -b:v:0 800k ")
//                    .append("-map 0:v -map 0:a -s:v:1 1280x720 -b:v:1 2800k ")
//                    .append("-map 0:v -map 0:a -s:v:2 1920x1080 -b:v:2 5000k ")
//                    .append("-var_stream_map \"v:0,a:0 v:1,a:0 v:2,a:0\" ")
//                    .append("-master_pl_name ")
//                    .append(HSL_DIR)
//                    .append(videoId)
//                    .append("/master.m3u8 ")
//                    .append("-f hls -hls_time 10 -hls_list_size 0 ")
//                    .append("-hls_segment_filename \"")
//                    .append(HSL_DIR)
//                    .append(videoId)
//                    .append("/v%v/fileSequence%d.ts\" ")
//                    .append("\"")
//                    .append(HSL_DIR)
//                    .append(videoId)
//                    .append("/v%v/prog_index.m3u8\"");

            log.info("ffmpeg command: {}", ffmpegCmd);
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", ffmpegCmd); //"/bin/bash", "-c" -> is used for linux
            processBuilder.inheritIO();
            Process process = processBuilder.start();

            int exist = process.waitFor();

            //print the process details
            ProcessHandle.Info info = process.info();
            log.info("Cmd status: {}", info);

            if (exist != 0) {
                throw new RuntimeException("Video Processing Failed!");
            }
            return uniqueId;
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Video Processing Failed!");
        }
    }

    @Override
    public ResponseEntity<?> executeMasterFile(Integer videoId) {
        //check videoId
        Video video = videoDao.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video ID is not exist!"));

        String uniqueId = video.getUniqueId();

        //create path
        Path videoPath = Paths.get(HLS_DIR, uniqueId, "master.m3u8");
        log.info("Video file path is: {}", videoPath);

        //check the path is valid
        if (!Files.exists(videoPath)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("master file path is not found");
        }

        Resource resource = new FileSystemResource(videoPath);

        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(resource);
    }

    @Override
    public ResponseEntity<?> executeSegmentFile(Integer videoId, String segment) {
        Video video = videoDao.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video ID is not exist!"));

        String uniqueId = video.getUniqueId();

        //create video path
        Path videoPath = Paths.get(HLS_DIR, uniqueId, segment + ".ts");

        if (!Files.exists(videoPath)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Segment file path is not found");
        }

        Resource resource = new FileSystemResource(videoPath);
        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, "video/mp2t")
                .body(resource);
    }

}
