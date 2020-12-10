/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.loader.smart;

import java.util.Scanner;

public class SmartPrompter {

    private static final Scanner scanner = new Scanner(System.in);


    public static void promptUser(String message){
        System.out.print(String.format("[KumuluzEE Smart Loader] %s", message));
    }

    public static String promptUserForInput(String message){
        System.out.print(String.format("[KumuluzEE Smart Loader] %s: ", message));
        return scanner.nextLine();
    }

    public static String promptUserForInputPersistant(String message){
        String input = "";
        while (input.isEmpty()){
            input = promptUserForInput(message);
        }

        return input;
    }

    public static boolean promptUserForBoolean(String message){
        String input = promptUserForInputPersistant(String.format("%s [y/n]", message)).toLowerCase();
        if (input.equals("y") || input.equals("yes")) {
            return true;
        }
        else if (input.equals("n") || input.equals("no")){
            return false;
        }
        else {
            return promptUserForBoolean(message);
        }
    }

}
