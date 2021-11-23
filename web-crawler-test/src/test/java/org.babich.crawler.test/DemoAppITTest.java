package org.babich.crawler.test;

import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class DemoAppITTest {

  static {
    //Path path = Paths.get(Paths.get("src/test/resources").toFile().getAbsolutePath());
    //Paths.get(StringUtils.EMPTY).toAbsolutePath().getParent().toAbsolutePath()
  }

  @Container
  public static GenericContainer<?> dslContainer = new GenericContainer<>(
      new ImageFromDockerfile()
         // .withFileFromClasspath("Dockerfile", "docker")
  );
  //"web-crawler-test/src/test/resources"

  @BeforeEach
  void setUp(){

  }

  @Test
  void start(){

    System.out.println(dslContainer.getCurrentContainerInfo().getState());


//    Path path = Paths.get(Paths.get("src/test/resources").toFile().getAbsolutePath());
    //System.out.println(path);
  }

}
