package uti.bot;

//Want to add more languages? you only need to edit this file!
//2 locations, add a new line up here where the languages are declared, then at the bottom add a new branch to the switch expression
public enum Language {
    //available replacements:
    //{PROBLEM_NAME}
    //{CONTAINER_USER_FILE} will be quoted
    //{TIMEOUT_SCORE} the score to use upon failure
    //{CONTAINER_SCORE_PATH}
    //{CONTAINER_OUTPUT_PATH}
    C("gcc","C","gcc {CONTAINER_USER_FILE} -o a.out -std=c17 2>&1", "./a.out","utibot/gcc-base:latest","c"),
    CPP("gcc","C++","g++ {CONTAINER_USER_FILE} -o a.out -std=c++20 2>&1", "./a.out","utibot/gcc-base:latest","cpp"),
    JAVA("java", "Java","", "java {CONTAINER_USER_FILE}", "utibot/java-base","java"),
    PY("python", "Python", "", "python3 {CONTAINER_USER_FILE}","utibot/python-base:latest","py")
    ;

    /**The docker image to use*/
    final String image;

    final String language;

    final String preProcessCommand;//ex: compile

    final String evaluationCommand;//full run command

    final String baseImage;
    final String extension;

    Language(String image, String language, String preProcessCommand, String evaluationCommand, String baseImage, String extension) {
        this.image = image;
        this.language = language;
        this.evaluationCommand = evaluationCommand;
        this.preProcessCommand = preProcessCommand;
        this.baseImage = baseImage;
        this.extension =extension;
    }

    public static Language fromFileExtension(String ext){
        return switch (ext){
            case "c" -> C;
            case "cpp", "c++" -> CPP;
            case "java" -> JAVA;
            case "py" -> PY;

            case null, default -> null;
        };
    }
}
