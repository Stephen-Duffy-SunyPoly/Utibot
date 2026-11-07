package uti.bot;

import com.github.dockerjava.api.DockerClient;
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

    private final ArrayList<String> createdContainerNames = new ArrayList<>();

    private boolean generatingContainers = false;
    private boolean generationRequested = false;

    private boolean shutdown = false;

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
            System.out.println("Docker cleanup complete.");
        }));
    }

    public void shutdown() {
        if(!shutdown) {
            shutdown = true;
            try {
                dockerClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void requestRegenerateImages(){
        synchronized (createdContainerNames) {
            generationRequested = true;
        }
        try {
            buildLanguageImages();
        } catch (IOException e) {
            generatingContainers =false;
            throw new RuntimeException(e);
        }
    }

    public synchronized void buildLanguageImages() throws IOException {
        if(!new File("problems/").exists()){
            //if no problems exists, then just do nothing
            return;
        }
        generatingContainers = true;
        synchronized (createdContainerNames){
            generationRequested = false;
        }
//        if(!createdContainerNames.isEmpty()){
//            throw new RuntimeException("Attempted to re create containers without deleting them first");
//        }
        createdContainerNames.clear();

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

        ArrayList<Thread> generatorThreads = new ArrayList<>();
        for(Language lang: Language.values()){
            String imageName = "utibot-submission-"+lang.image+"_app";
            if(!createdContainerNames.contains(lang.image)) {
            generatorThreads.add(new Thread( () -> {
                    //if the image already exists (like from a previous run) delete it
                    for (Image image : images) {
                        String[] tags = image.getRepoTags();

                        if (tags != null && Arrays.asList(tags).contains(imageName)) {
                            dockerClient.removeImageCmd(image.getId())
                                    .withForce(false)
                                    .exec();
                        }
                    }

                    System.out.println("building image for: " + lang.language);
                    String imageId = dockerClient.buildImageCmd()
                            .withDockerfile(dockerfile)
                            .withBaseDirectory(buildContext)
                            .withBuildArg("BASE_IMAGE", lang.baseImage)
                            .withTags(Set.of(imageName))
                            .start()
                            .awaitImageId();
                    System.out.println("Built image: " + imageId+" ("+lang.language+")");
                }));
                generatorThreads.getLast().start();
            }
            createdContainerNames.add(lang.image);
        }
        for(Thread t:generatorThreads){
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        //if in the process of generation, another generation request came in
        cleanUpDanglingImages();
        if(!generationRequested){
            generatingContainers = false;
        }
    }

    public void deleteLanguageContainers(){
        for(String image: createdContainerNames){
            dockerClient.removeImageCmd("utibot-submission-"+image+"_app").exec();
        }
        createdContainerNames.clear();
    }

    public void runContainer(String imageName, String command, HostConfig hostConfig){
        while(generatingContainers){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        String containerId = null;
        try {
            var container = dockerClient.createContainerCmd(imageName)
                    .withCmd("bash","-c",command)
                    .withHostConfig(hostConfig)
                    .exec();
            containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            long statusCode = dockerClient.waitContainerCmd(containerId)
                    .start()
                    .awaitStatusCode();
            System.out.println("Container status code: "+statusCode);
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

    public void cleanUpDanglingImages(){
        List<Image> danglingImages = dockerClient.listImagesCmd()
                .withDanglingFilter(true)
                .exec();

        for (Image image : danglingImages) {
            try {
                dockerClient.removeImageCmd(image.getId())
                    .withForce(false)
                    .exec();
            } catch (Exception e) {
                System.err.println(
                        "Could not remove image " + image.getId()
                                + ": " + e.getMessage()
                );
            }
        }
    }

}
