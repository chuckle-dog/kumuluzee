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

import java.lang.reflect.Method;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author Aljaž Pavišič
 *
 */
public class EeSmartLoader {

    private static String RESOLVE_DEPENDENCIES = "resolveDependencies";

    public static void main(String[] args) throws Throwable {

        if (args.length > 1) {
            SmartPrompter.promptUser("Too many arguments. Use 1 or none");
            return;
        }

        try {
            ResourceBundle bootLoaderProperties = ResourceBundle.getBundle("META-INF/kumuluzee/boot-loader");

            String mainClass = bootLoaderProperties.getString("main-class");
            String[] repositoryArray = bootLoaderProperties.getString("repository-paths").split(",");
            String[] dependencyArray = bootLoaderProperties.getString("dependency-paths").split(",");

            List<String> missingDeps = DependencyResolver.checkDependencies(dependencyArray);

            if (args.length == 0) {

                if (missingDeps.size() == 0){
                    ClassLoader classLoader = EeSmartLoader.class.getClassLoader();
                    Class<?> clazz = classLoader.loadClass(mainClass);

                    Method method = clazz.getMethod("main", String[].class);

                    if (method != null){
                        method.invoke(null, (Object)args);
                    }
                }
                else {
                    SmartPrompter.promptUser("One or more dependencies are missing. Terminating boot process.");
                }
            }
            else if (args[0].equals(RESOLVE_DEPENDENCIES)){
                if (DependencyResolver.resolveDependencies(repositoryArray, missingDeps)){
                    SmartPrompter.promptUser("All dependencies downloaded. You may now run the application normally.");
                }
                else {
                    SmartPrompter.promptUser("One or more dependencies could not be downloaded.");
                }
            }

        } catch (MissingResourceException e) {
            e.printStackTrace();
        }
    }
}
