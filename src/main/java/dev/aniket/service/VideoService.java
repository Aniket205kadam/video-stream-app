package dev.aniket.service;


import dev.aniket.model.Video;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface VideoService {
    //save video
    Video save(Video video, MultipartFile file);

    //get video by id
    Video getById(Integer videoId);

    //get video by title
    Video getByTitle(String title);

    //get all video
    List<Video> getAll();

    //get video's by ids
    List<Video> getAllByIds(List<Integer> ids);

    //video processing
//    String processVideo(Integer videoId);

    String processVideo(Path path);

    ResponseEntity<?> executeMasterFile(Integer videoId);

    ResponseEntity<?> executeSegmentFile(Integer videoId, String segment);
}
