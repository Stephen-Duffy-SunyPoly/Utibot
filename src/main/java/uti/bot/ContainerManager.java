package uti.bot;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class ContainerManager {

    public enum BaseImage{
        GCC_BASE("gcc g++ libc6-dev binutils","gcc"),
        JAVA_BASE("openjdk-17-jdk-headless","java");

        final String installPackages;
        final String imageName;

        BaseImage(String installPackages, String imageName){
            this.imageName = imageName;
            this.installPackages = installPackages;
        }

        public String getImageName(){
            return "utibot/"+imageName+"-base";
        }

    }

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

    public boolean imageExists(String imageName) {
        try {
            // Attempt to inspect the image by its name (e.g., "ubuntu:latest") or ID
            InspectImageResponse response = dockerClient.inspectImageCmd(imageName).exec();
            return response != null;
        } catch (NotFoundException e) {
            // The library throws NotFoundException if the image does not exist locally
            return false;
        }
    }

    public void buildBaseImages(boolean force){
        //check if the base python image exsists
        if(!imageExists("utibot/python-base:latest") || force) {
            //if not,
            //create a folder structure for the images to be built from
            boolean ignored = new File("base/python").mkdirs();
            ignored = new File("base/gcc").mkdirs();
            ignored = new File("base/java").mkdirs();
            //extract all the docker files
            try(InputStream extractDockerFile = ContainerManager.class.getClassLoader().getResourceAsStream("pythonBaseDockerFile")){
                try(FileOutputStream dockerFileOut = new FileOutputStream("base/python/DockerFile")){
                    assert extractDockerFile != null;
                    extractDockerFile.transferTo(dockerFileOut);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //build the python image
            System.out.println("Building python base image");
            dockerClient.buildImageCmd()
                    .withDockerfile(new File("base/python/DockerFile"))
                    .withBaseDirectory(new File("base/python/"))
                    .withTags(Set.of("utibot/python-base"))
                    .start()
                    .awaitImageId();
            //build the other language images
            System.out.println("Python base image complete");
            //do the dynamic images
            for(BaseImage base: BaseImage.values()){
                System.out.println("Building "+base.imageName+" base image");
                //extract and create the docker file
                try(InputStream extractDockerFile = ContainerManager.class.getClassLoader().getResourceAsStream("baseDockerFile")){
                    assert extractDockerFile != null;
                    String dockerFileString = new String(extractDockerFile.readAllBytes(), StandardCharsets.UTF_8);
                    dockerFileString = dockerFileString.replace("{PACKAGES}",base.installPackages);
                    try(PrintWriter dockerFileOut = new PrintWriter(new FileOutputStream("base/"+base.imageName+"/DockerFile"))) {
                        dockerFileOut.write(dockerFileString);
                        dockerFileOut.flush();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //create the container
                dockerClient.buildImageCmd()
                    .withDockerfile(new File("base/"+base.imageName+"/DockerFile"))
                    .withBaseDirectory(new File("base/"+base.imageName))
                    .withTags(Set.of(base.getImageName()))
                    .start()
                    .awaitImageId();
                System.out.println(base.imageName+" base image complete");
            }
            System.out.println("Finished building base images");
            //delete the folder structure
            FileUtil.recursiveDelete(new File("base/"));
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
        //delete the docker file
        boolean ignored2 = dockerfile.delete();
        if(!generationRequested){
            generatingContainers = false;
            try {
                if (Main.config.isServerSetup() && Main.competitionServer != null) {
                    Objects.requireNonNull(Main.competitionServer.getTextChannelById(Main.config.getAdminChannelId())).sendMessage("Container Images finished reloading").queue();
                }
            } catch (Exception ignored){}
        }
    }

    public void deleteLanguageContainers(){
        for(String image: createdContainerNames){
            try {
                dockerClient.removeImageCmd("utibot-submission-" + image + "_app").exec();
            } catch (Exception e){
                e.printStackTrace();
            }
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
//            System.out.println("Container status code: "+statusCode);
        } finally {
            try {
                if (containerId != null) {
                    dockerClient.removeContainerCmd(containerId)
                            .withForce(true)
                            .exec();
                }
            } catch (Exception ignored){}
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
