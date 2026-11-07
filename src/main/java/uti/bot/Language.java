package uti.bot;

public enum Language {
    //available replacements:
    //{PROBLEM_NAME}
    //{CONTAINER_USER_FILE} will be quoted
    //{TIMEOUT_SCORE} the score to use upon failure
    //{CONTAINER_SCORE_PATH}
    //{CONTAINER_OUTPUT_PATH}
    C("gcc","C","gcc {CONTAINER_USER_FILE} -o a.out -std=c17", "./a.out"),
    CPP("gcc","C++","g++ {CONTAINER_USER_FILE} -o a.out -std=c++20", "./a.out"),
    JAVA("java", "Java","", "java {CONTAINER_USER_FILE}"),
    PY("python", "Python", "", "python3 {CONTAINER_USER_FILE}")
    ;

    /**The docker image to use*/
    final String image;

    final String language;

    final String preProcessCommand;//ex: compile

    final String evaluationCommand;//full run command

    Language(String image, String language, String preProcessCommand, String evaluationCommand) {
        this.image = image;
        this.language = language;
        this.evaluationCommand = evaluationCommand;
        this.preProcessCommand = preProcessCommand;
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
