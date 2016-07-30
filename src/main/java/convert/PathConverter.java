package convert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class PathConverter {

    private final String jsonFile;
    private final boolean keepCopy;

    public PathConverter(String jsonFile, boolean keepCopy) {
        this.jsonFile = jsonFile;
        this.keepCopy = keepCopy;
    }

    public final void convert() {
        System.out.println("Converting: " + jsonFile);
        String parentFile = new File(jsonFile).getAbsoluteFile().getParentFile().toString();
        if (new File(parentFile + File.separator + "files").exists()) {
            System.out.println(new File(parentFile + File.separator + "files"));
            System.out.println("Files directory already exists. Do not copy anything.");
            return;
        }
        if (keepCopy) {
            try {
                Files.copy(Paths.get(jsonFile), Paths.get(parentFile + File.separator + "orig-" + new File(jsonFile)
                        .getName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File filePath = new File(parentFile + File.separator + "files");
        if (!filePath.mkdir()) {
            System.err.println("Could not setup files directory '" + filePath + "'");
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(jsonFile));
        } catch (IOException e) {
            e.printStackTrace();
            lines = new ArrayList<>();
        }
        String fileContent = lines.parallelStream().reduce((s1, s2) -> s1 + s2).orElse("");
        JSONArray outputArray = new JSONArray();
        boolean write = true;
        if (jsonFile.contains("tests-engine-dependent.json")) {
            outputArray = copyEngineDependentFiles(parentFile, fileContent);
        } else if (jsonFile.contains("tests-engine-independent.json")) {
            outputArray = getCopyEngineIndependentFiles(parentFile, fileContent);

        } else {
            System.out.println("The file is not a engine dependent or independent file.");
            write = false;
        }

        try {
            if (write) {
                Files.write(Paths.get(jsonFile), outputArray.toString(1).getBytes());
            }
        } catch (IOException e) {
            System.err.println("Could not write " + jsonFile);
        }
    }

    private JSONArray getCopyEngineIndependentFiles(String parentFile, String fileContent) {
        JSONArray inputArray = new JSONArray(fileContent);
        JSONArray outputArray = new JSONArray();
        for (int i = 0; i < inputArray.length(); i++) {
            JSONObject obj = inputArray.getJSONObject(i);
            String featureId = obj.getString(Constants.FEATURE_ID_TOKEN);

            JSONArray engineIndependentArrayOutput = copyEngineIndependentFiles(parentFile, obj
                    .getJSONArray(Constants.ENGINE_INDEPENDENT_FILES_TOKEN), featureId);
            obj.remove(Constants.ENGINE_INDEPENDENT_FILES_TOKEN);
            obj.put(Constants.ENGINE_INDEPENDENT_FILES_TOKEN, engineIndependentArrayOutput);

            outputArray.put(obj);
        }
        return outputArray;
    }

    /**
     * Copies the engine dependent files and replaces the file paths in the json file
     *
     * @param parentFile  path to the directory where the json file is
     * @param fileContent content of the json file
     */
    private JSONArray copyEngineDependentFiles(String parentFile, String fileContent) {
        JSONArray inputArray = new JSONArray(fileContent);
        JSONArray outputArray = new JSONArray();
        for (int i = 0; i < inputArray.length(); i++) {
            JSONObject obj = inputArray.getJSONObject(i);
            String engine = obj.getString(Constants.ENGINE_ID_TOKEN);
            String featureId = obj.getString(Constants.FEATURE_ID_TOKEN);

            JSONArray engineDependentArrayOutput = copyEngineDependentFiles(parentFile, obj
                    .getJSONArray(Constants.ENGINE_DEPENDENT_FILES_TOKEN), engine, featureId);
            obj.remove(Constants.ENGINE_DEPENDENT_FILES_TOKEN);
            obj.put(Constants.ENGINE_DEPENDENT_FILES_TOKEN, engineDependentArrayOutput);

            JSONArray logFileArrayOutput = copyLogFiles(parentFile, obj.getJSONArray(Constants.LOG_FILES_TOKEN), engine, featureId);
            obj.remove(Constants.LOG_FILES_TOKEN);
            obj.put(Constants.LOG_FILES_TOKEN, logFileArrayOutput);
            outputArray.put(obj);
        }
        return outputArray;
    }

    /**
     * Copies the engine dependent files from the original destination to the new destination.<br>
     * Path is : parentFile/files/logs/<engine>/<feature>Id
     *
     * @param parentFile                  path to the directory where the original json file is.
     * @param engineIndependentArrayInput the original files to be changed
     * @param featureId                   of the test with the files to be changed
     * @return the changed files as an JsonArray
     */
    private JSONArray copyEngineIndependentFiles(String parentFile, JSONArray engineIndependentArrayInput,
                                                 String featureId) {


        JSONArray engineIndependentArrayOutput = new JSONArray();
        for (int j = 0; j < engineIndependentArrayInput.length(); j++) {
            String oldPath = engineIndependentArrayInput.getString(j);
            if ((parentFile.endsWith(File.separator + "test") || parentFile.endsWith("\\" + "test")) && oldPath.startsWith("test")) {
                oldPath = oldPath.replace("test" + File.separator, "").replace("test" + "\\", "");
            }
            if (oldPath.endsWith(".bpmn")) {
                engineIndependentArrayInput.put(createBPMNImage(parentFile, oldPath));
            }
            String newPath = "files" + File.separator + "engineIndependent" + File.separator + Math.abs(featureId.hashCode());
            newPath += File.separator + oldPath.substring(convertToPath(oldPath).lastIndexOf(File.separator) + 1);

            if (copyFile(parentFile, oldPath, newPath)) engineIndependentArrayOutput.put(newPath);


        }
        return engineIndependentArrayOutput;
    }

    /**
     * Copies the log files from the original destination to the new destination.<br>
     * Path is : parentFile/files/logs/<engine>/<feature>Id
     *
     * @param parentFile        path to the directory where the original json file is.
     * @param logFileArrayInput the original lof files to be changed
     * @param engine            of the test with the lof files to be changed
     * @param featureId         of the test with the log files to be changed
     * @return the changed log files as an JsonArray
     */
    private JSONArray copyLogFiles(String parentFile, JSONArray logFileArrayInput, String engine, String featureId) {
        JSONArray logFileArrayOutput = new JSONArray();
        for (int i = 0; i < logFileArrayInput.length(); i++) {
            String p = logFileArrayInput.getString(i);
            String newPath = "files" + File.separator + "logs" + File.separator + engine + File.separator + Math.abs(featureId.hashCode()) + File.separator;
            newPath += p.substring(convertToPath(p).lastIndexOf(File.separator) + 1);
            if (copyFile(parentFile, p, newPath)) {
                logFileArrayOutput.put(newPath);
            }
        }
        return logFileArrayOutput;
    }

    /**
     * Copies the engine dependent files from the original destination to the new destination.<br>
     * Path is : parentFile/files/logs/<engine>/<feature>Id
     *
     * @param parentFile                path to the directory where the original json file is.
     * @param engineDependentArrayInput the original files to be changed
     * @param engine                    of the test with the files to be changed
     * @param featureId                 of the test with the files to be changed
     * @return the changed files as an JsonArray
     */
    private JSONArray copyEngineDependentFiles(String parentFile, JSONArray engineDependentArrayInput, String engine,
                                               String featureId) {
        JSONArray engineDependentArrayOutput = new JSONArray();
        for (int j = 0; j < engineDependentArrayInput.length(); j++) {
            String oldPath = engineDependentArrayInput.getString(j);
            if ((parentFile.endsWith(File.separator + "test") || parentFile.endsWith("\\" + "test")) && oldPath.startsWith("test")) {
                oldPath = oldPath.replace("test" + File.separator, "").replace("test" + "\\", "");
            }
            if (oldPath.endsWith(".bpmn")) {
                engineDependentArrayInput.put(createBPMNImage(parentFile, oldPath));
            }
            String newPath = "files" + File.separator + "engineDependent" + File.separator + engine + File.separator + Math.abs(featureId.hashCode());

            newPath += File.separator + oldPath.substring(convertToPath(oldPath).lastIndexOf(File.separator) + 1);
            if (copyFile(parentFile, oldPath, newPath))
                engineDependentArrayOutput.put(newPath);
        }
        return engineDependentArrayOutput;
    }

    private String createBPMNImage(String parentDir, String oldPath) {
        File bpmnFile = new File(convertToPath(parentDir + File.separator + oldPath));
        String[] args = {bpmnFile.getParentFile().toString()};
        try {
            bpmnviz.Main.main(args);
        } catch (IOException e) {

        }
        return oldPath + ".png";
    }


    /**
     * Copies the file from the oldPath to the newPath in the specified parent directory
     *
     * @param parentDir the parent directory of the file
     * @param oldPath   to of the file to be copied
     * @param newPath   of the file to be copied
     * @return true when copy was successfull, false otherwise
     */
    private boolean copyFile(String parentDir, String oldPath, String newPath) {
        File f = new File(parentDir + File.separator + newPath).getParentFile();
        if (!f.exists()) {
            f.mkdirs();
        }
        if ((parentDir.endsWith(File.separator + "test") || parentDir.endsWith("\\" + "test")) && oldPath.startsWith("test")) {
            oldPath = oldPath.replace("test" + File.separator, "").replace("test" + "\\", "");
        }
        Path source = Paths.get(convertToPath(parentDir) + File.separator + convertToPath(oldPath));
        Path target = Paths.get(convertToPath(parentDir) + File.separator + convertToPath(newPath));
        if (!source.equals(target)) {
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Could not copy file: " + source.toString() + " -> " + target.toString());
                System.err.println("Reason: " + e.getMessage());
            }
        }
        return true;
    }

    private String convertToPath(String path) {
        return path.replace("\\", File.separator).replace("/", File.separator);
    }
}