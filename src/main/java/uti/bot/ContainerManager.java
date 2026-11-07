package uti.bot;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Set;

public class ContainerManager {

    private DockerClientConfig standard;
    private DockerHttpClient httpClient;

    private DockerClient dockerClient;

    private static ContainerManager instance;

    private ArrayList<String> createdContainerNames = new ArrayList<>();

    public static synchronized void init(){
        if(instance == null){
            instance = new ContainerManager();
        }
    }

    public static ContainerManager getInstance(){
        return instance;
    }

    private ContainerManager(){
        //these can be set through environment vars:
        /*
        export DOCKER_HOST=tcp://localhost:2376
        export DOCKER_TLS_VERIFY=1
        export DOCKER_CERT_PATH=/home/user/.docker/certs
        export DOCKER_CONFIG=/home/user/.docker
         */
        standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        httpClient = new ApacheDockerHttpClient.Builder().dockerHost(standard.getDockerHost())
                .sslConfig(standard.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        dockerClient = DockerClientImpl.getInstance(standard, httpClient);
        dockerClient.pingCmd().exec();
    }

    public void buildLanguageContainers() throws IOException {
        if(!createdContainerNames.isEmpty()){
            throw new RuntimeException("Attempted to re create containers without deleting them first");
        }
        //extract the docker file!
        File buildContext = new File(".").getCanonicalFile();//the dir with the docker file in it
        File dockerfile = new File(buildContext, "Dockerfile");
        try(InputStream extractDockerFile = instance.getClass().getResourceAsStream("Dockerfile")){
            try(FileOutputStream dockerFileOut = new FileOutputStream(dockerfile)){
                assert extractDockerFile != null;
                extractDockerFile.transferTo(dockerFileOut);
            }
        }

        for(Language lang: Language.values()){
            if(!createdContainerNames.contains(lang.image)){
                String imageId = dockerClient.buildImageCmd()
                        .withDockerfile(dockerfile)
                        .withBaseDirectory(buildContext)
                        .withBuildArg("BASE_IMAGE", lang.baseImage)
                        .withTags(Set.of("utibot-submission-"+lang.image+"_app"))
                        .start()
                        .awaitImageId();

                createdContainerNames.add(lang.image);
                System.out.println("Built image: " + imageId);
            }
        }
    }

    public void deleteLanguageContainers(){
        for(String image: createdContainerNames){
            dockerClient.removeImageCmd("utibot-submission-"+image+"_app").exec();
        }
        createdContainerNames.clear();
    }

}
