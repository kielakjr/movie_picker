package com.kielakjr.movie_picker;

import com.kielakjr.movie_picker.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class MoviePickerApplicationIT {

	@Test
	void contextLoads() {
	}

}
