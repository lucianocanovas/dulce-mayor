package ing.software.dulcemayor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DulceMayorApplication {

	public static void main(String[] args) {
		SpringApplication.run(DulceMayorApplication.class, args);
	}

}
