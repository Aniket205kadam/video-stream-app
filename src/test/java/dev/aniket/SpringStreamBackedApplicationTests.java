package dev.aniket;

import dev.aniket.model.Video;
import dev.aniket.service.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringStreamBackedApplicationTests {

	private VideoService videoService;

	@Autowired
	public SpringStreamBackedApplicationTests(VideoService videoService) {
		this.videoService = videoService;
	}

	@Test
	void processVideoTest() {
//		videoService.processVideo(16);
	}

	@Test
	void getVideByIdTest() {
		Video video = videoService.getById(16);
		System.out.println(video);
	}

}
