package uti.bot;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        if(!new File("problems/").exists()){
            //if no problems exists, then just do nothing
            return;
        }
        if(!createdContainerNames.isEmpty()){
            throw new RuntimeException("Attempted to re create containers without deleting them first");
        }

        System.out.println("regenerating container images");

        //extract the docker file!
        File buildContext = new File(".").getCanonicalFile();//the dir with the docker file in it
        File dockerfile = new File(buildContext, "Dockerfile");
        try(InputStream extractDockerFile = ContainerManager.class.getClassLoader().getResourceAsStream("Dockerfile")){
            try(FileOutputStream dockerFileOut = new FileOutputStream(dockerfile)){
                assert extractDockerFile != null;
                extractDockerFile.transferTo(dockerFileOut);
            }
        }
        createDockerIgnore();

        List<Image> images = dockerClient.listImagesCmd()
                .withShowAll(true)
                .exec();

        for(Language lang: Language.values()){
            String imageName = "utibot-submission-"+lang.image+"_app";

            if(!createdContainerNames.contains(lang.image)){
                //if the image already exists (like from a previous run) delete it
                for (Image image : images) {
                    String[] tags = image.getRepoTags();

                    if (tags != null && Arrays.asList(tags).contains(imageName)) {
                        dockerClient.removeImageCmd(image.getId())
                                .withForce(true)
                                .exec();
                    }
                }

                System.out.println("building image for: "+lang.language);
                String imageId = dockerClient.buildImageCmd()
                        .withDockerfile(dockerfile)
                        .withBaseDirectory(buildContext)
                        .withBuildArg("BASE_IMAGE", lang.baseImage)
                        .withTags(Set.of(imageName))
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

    public void runContainer(String imageName, String command, HostConfig hostConfig){
        String containerId = null;
        try {
            var container = dockerClient.createContainerCmd(imageName)
                    .withCmd(command)
                    .withHostConfig(hostConfig)
                    .exec();
            containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            dockerClient.waitContainerCmd(containerId)
                    .start()
                    .awaitStatusCode();
        } finally {
            if (containerId != null) {
                dockerClient.removeContainerCmd(containerId)
                        .withForce(true)
                        .exec();
            }
        }
    }

    public void createDockerIgnore(){
        //check if exsists
        if(!new File(".dockerignore").exists()) {
            try (PrintWriter pr = new PrintWriter(".dockerignore")) {
                pr.println("**");
                pr.println("!Dockerfile");
                pr.println("!problems");
                pr.println("!problems/**");
                pr.flush();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void deleteDockerIgnore(){
       boolean ignored = new File(".dockerignore").delete();
    }

}
