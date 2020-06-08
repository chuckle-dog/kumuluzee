package com.kumuluz.ee.common.utils;

import java.util.Scanner;

public class Prompter {

    private static final Scanner scanner = new Scanner(System.in);

    static String promptUserForInput(String request){

        System.out.print(String.format("[KumuluzEE project generator] %s: ", request));
        String input = scanner.nextLine();

        return input;
    }

    public static String promptUserForInputPersistant(String request){
        String input = "";
        while (input.isBlank()){
            input = promptUserForInput(request);
        }

        return input;
    }

    public static String promptUserForInputWithDefault(String request, String def){
        String input = promptUserForInput(String.format("%s [default=%s]", request, def));
        if (input.isBlank()){
            input = def;
        }

        return input;
    }

    public static boolean promptUserForBoolean(String request){
        String input = promptUserForInputPersistant(String.format("%s [y/n]", request)).toLowerCase();
        if (input.equals("y") || input.equals("yes")){
            return true;
        }
        else if (input.equals("n") || input.equals("no")){
            return false;
        }
        else {
            return promptUserForBoolean(request);
        }
    }

    public static KumuluzProject promptUserForProjectInfo(KumuluzProject kumuluzProject){

        String groupId = promptUserForInputPersistant("Enter groupId");
        String artifactId = promptUserForInputPersistant("Enter artifactId");
        String projectVersion = promptUserForInputWithDefault("Enter project version", "1.0-SNAPSHOT");
        String kumuluzVersion = promptUserForInputWithDefault("Enter KumuluzEE version", "3.10.0-SNAPSHOT");
        String name = artifactId;

        kumuluzProject.setName(name);
        kumuluzProject.setGroupId(groupId);
        kumuluzProject.setArtifactId(artifactId);
        kumuluzProject.setVersion(projectVersion);
        kumuluzProject.setKumuluzVersion(kumuluzVersion);

        return kumuluzProject;
    }

}
