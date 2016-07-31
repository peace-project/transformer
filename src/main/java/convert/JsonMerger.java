package convert;


import org.fife.ui.rsyntaxtextarea.Style;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Merges the json files.
 * <ul>
 * <li>directories</li>
 * <li>tests-engine-dependent.json</li>
 * <li>tests-engine-independent.json</li>
 * <li>feature-tree.json</li>
 * <li>engines.json</li>
 * </ul>
 */
public class JsonMerger {

    /**
     * newer path. all objects will be used.
     */
    private final String newerPath;

    /**
     * older path. oly objects that are not in the {@link #newerPath} will be used.
     */
    private final String olderPath;

    /**
     * target directory.
     */
    private String targetDir;

    /**
     * merge mode.
     */
    private int mode;

    /**
     * mode directory. merges directory.
     */
    private final static int MODE_DIRECTORY = 1;

    /**
     * mode engine dependent. merges tests-engine-dependent.json
     */
    private final static int MODE_ENGINE_DEPENDENT = 2;

    /**
     * mode engine independent. merges tests-engine-independent.json
     */
    private final static int MODE_ENGINE_INDEPENDENT = 3;

    /**
     * mode feature-tree. merges feature-tree.json
     */
    private static final int MODE_FEATURE_TREE = 4;

    /**
     * mode engines. merges engines.json
     */
    private static final int MODE_ENGINES = 5;

    /**
     * List of the replaced objects.
     */
    private final List<String> replacedObjects = new ArrayList<>();

    /**
     * List of the new objects.
     */
    private final List<String> newObjects = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param olderPath to be merged
     * @param newerPath to be merged
     */
    public JsonMerger(String olderPath, String newerPath) {
        this.olderPath = olderPath;
        this.newerPath = newerPath;
        File oldPath = new File(olderPath);
        File newPath = new File(newerPath);
        if (oldPath.isDirectory() && newPath.isDirectory()) {
            mode = MODE_DIRECTORY;
        } else if (oldPath.getName().contains("tests-engine-dependent.json") && newPath.getName().contains("tests-engine-dependent.json")) {
            mode = MODE_ENGINE_DEPENDENT;
        } else if (oldPath.getName().contains("tests-engine-independent.json") && newPath.getName().contains("tests-engine-independent.json")) {
            mode = MODE_ENGINE_INDEPENDENT;
        } else if (oldPath.getName().contains("feature-tree.json") && newPath.getName().contains("feature-tree.json")) {
            mode = MODE_FEATURE_TREE;
        } else if (oldPath.getName().contains("engines.json") && newPath.getName().contains("engines.json")) {
            mode = MODE_ENGINES;
        }
        if (mode == MODE_DIRECTORY) {
            targetDir = olderPath;
        } else {
            targetDir = new File(olderPath).getParent();
        }
    }

    /**
     * Constructor
     *
     * @param olderPath to be merged
     * @param newerPath to be merged
     * @param targetDir to generate the files to
     */
    public JsonMerger(String olderPath, String newerPath, String targetDir) {
        this(olderPath, newerPath);
        this.targetDir = targetDir;
    }

    /**
     * merges the json's.
     */
    public void merge() {
        newObjects.clear();
        replacedObjects.clear();
        switch (mode) {
            case MODE_DIRECTORY:
                mergeDirectories();
                break;
            case MODE_ENGINE_DEPENDENT:
                mergeEngineDependent();
                break;
            case MODE_ENGINE_INDEPENDENT:
                mergeEngineIndependent();
                break;
            case MODE_FEATURE_TREE:
                mergeFeatureTree();
                break;
            case MODE_ENGINES:
                mergeEngines();
                break;
        }
        System.out.println("------------------");
        System.out.println("Replaced " + replacedObjects.size() + " objects:");
        replacedObjects.forEach(System.out::println);
        System.out.println("------------------");
        System.out.println("Added " + newObjects.size() + " objects:");
    }

    /**
     * merges the engines.json
     */
    private void mergeEngines() {
        String oldLine = readLines(olderPath);
        String newLine = readLines(newerPath);
        JSONArray oldArray = new JSONArray(oldLine);
        JSONArray newArray = new JSONArray(newLine);
        System.out.println("Read " + oldArray.length() + " old engines.");
        System.out.println("Read " + newArray.length() + " new engines.");
        JSONArray outputArray = new JSONArray();
        for (int i = 0; i < newArray.length(); i++) {
            outputArray.put(newArray.get(i));
        }
        for (int i = 0; i < oldArray.length(); i++) {
            JSONObject oldTest = oldArray.getJSONObject(i);
            if (!containsEngineId(newArray, oldTest.getString(Constants.ID_TOKEN))) {
                outputArray.put(oldTest);
                newObjects.add(oldTest.getString(Constants.ID_TOKEN));
            } else {
                replacedObjects.add(oldTest.getString(Constants.ID_TOKEN));
            }
        }
        try {
            System.out.println("Writing " + outputArray.length() + " engines.");
            Files.write(Paths.get(targetDir + File.separator + "merged-engines.json"), outputArray.toString(1).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Checks if the array of engine objects contains an engine with the id.
     *
     * @param testObjects array of engine objects to be checked
     * @param id          to be checked
     * @return true when the id is in the array, false otherwise
     */
    private boolean containsEngineId(JSONArray testObjects, String id) {
        for (int i = 0; i < testObjects.length(); i++) {
            if (testObjects.getJSONObject(i).get(Constants.ID_TOKEN).equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * merges the feature-tree.json
     */
    private void mergeFeatureTree() {
        String oldLine = readLines(olderPath);
        String newLine = readLines(newerPath);
        JSONArray oldArray = new JSONArray(oldLine);
        JSONArray newArray = new JSONArray(newLine);
        System.out.println("Read " + oldArray.length() + " old constructs.");
        System.out.println("Read " + newArray.length() + " new constructs.");
        JSONArray outputArray = new JSONArray();
        for (int i = 0; i < newArray.length(); i++) {

            outputArray.put(newArray.getJSONObject(i));
        }
        for (int i = 0; i < oldArray.length(); i++) {
            JSONObject oldCap = oldArray.getJSONObject(i);
            JSONObject newCap = getObjectWithId(outputArray, oldCap.getString("id"));
            if (newCap == null) {
                //capability not present
                outputArray.put(oldCap);
                System.out.println("added " + oldCap.getString("id"));
                System.out.println();

            } else {
                System.out.println("both present merging: " + oldCap.getString("id"));
                System.out.println();

                String nextToken = getNextToken("language");
                JSONArray oldNextLevel = oldCap.getJSONArray(nextToken);
                JSONArray newNextLevel = newCap.getJSONArray(nextToken);
                oldCap.remove(nextToken);
                oldCap.put(nextToken, mergeLGCF(oldNextLevel, newNextLevel, nextToken));

            }
        }
        try {

            System.out.println("Writing " + outputArray.length() + " constructs.");
            Files.write(Paths.get(targetDir + File.separator + "merged-feature-tree.json"), outputArray.toString(1).getBytes());
            System.out.println("done writing.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONArray mergeLGCF(JSONArray oldArray, JSONArray newArray, String currentLevel) {
        JSONArray output = new JSONArray();
        for (int i = 0; i < newArray.length(); i++) {
            output.put(newArray.getJSONObject(i));

        }
        for (int i = 0; i < oldArray.length(); i++) {
            JSONObject oldObj = oldArray.getJSONObject(i);
            JSONObject newObj = getObjectWithId(output, oldObj.getString("id"));
            if (newObj == null) {
                output.put(oldObj);
                System.out.println("added " + oldObj.getString("id"));
                System.out.println();
                if(getNextToken(currentLevel)!=null) {
                    JSONObject old = oldObj;
                    String nextT = getNextToken(currentLevel);
                    JSONArray oldA = new JSONArray();
                    boolean make = true;
                    while(make) {
                        if (old != null) {
                            oldA = old.getJSONArray(nextT);
                        }
                        for (int j = 0; j < oldA.length(); j++) {
                            old = oldA.getJSONObject(j);
                            output.put(old);

                            System.out.println("added " + old.getString("id"));
                            System.out.println();
                            nextT = getNextToken(nextT);

                            if (nextT == null) {
                                //feature level
                                make=false;
                            }
                        }
                        }
                    }
            } else {
                //id already present => merging next level if present
                String nextToken = getNextToken(currentLevel);

                if (nextToken == null) {
                    //feature level
                    continue;
                }

                System.out.println("both present merging: " + oldObj.getString("id"));
                System.out.println();
                JSONArray oldNextLevel = oldObj.getJSONArray(nextToken);
                JSONArray newNextLevel = newObj.getJSONArray(nextToken);
                oldObj.remove(nextToken);
                //recursive step merging the next level
                oldObj.put(nextToken, mergeLGCF(oldNextLevel, newNextLevel, nextToken));
            }
        }
        return output;
    }

    private String getNextToken(String token) {
        switch (token) {
            case "features":
                return null;
            case "constructs":
                return "features";
            case "groups":
                return "constructs";
            case "languages":
                return "groups";
            default:
                return "languages";
        }
    }

    private JSONObject getObjectWithId(JSONArray newArray, String id) {
        for (int i = 0; i < newArray.length(); i++) {
            JSONObject jsonObject = newArray.getJSONObject(i);
            if (jsonObject.getString("id").equals(id)) {
                return jsonObject;
            }
        }
        return null;
    }

    private JSONObject getConstruct(String group, String name, JSONArray newArray) {
        for (int i = 0; i < newArray.length(); i++) {
            JSONObject obj = newArray.getJSONObject(i);
            if (obj.getString(Constants.GROUP_TOKEN).equals(group) || obj.getString(Constants.NAME_TOKEN).equals(name)) {
                return obj;
            }
        }
        return null;
    }

    /**
     * merges the engines-test-independent.json
     */
    private void mergeEngineIndependent() {
        new PathConverter(olderPath, true).convert();
        String oldLine = readLines(olderPath);
        new PathConverter(newerPath, true).convert();
        String newLine = readLines(newerPath);
        JSONArray oldArray = new JSONArray(oldLine);
        JSONArray newArray = new JSONArray(newLine);
        System.out.println("Read " + oldArray.length() + " old engine independent tests.");
        System.out.println("Read " + newArray.length() + " new engine independent tests.");
        JSONArray outputArray = new JSONArray();
        for (int i = 0; i < newArray.length(); i++) {
            String newParentPath = new File(newerPath).getAbsoluteFile().getParent();
            JSONObject newTest = oldArray.getJSONObject(i);
            if (!newerPath.equals(targetDir)) {
                copyFiles(newParentPath, newTest);
            }
            outputArray.put(newArray.get(i));
        }
        String oldParentPath = new File(olderPath).getAbsoluteFile().getParent();
        for (int i = 0; i < oldArray.length(); i++) {
            JSONObject oldTest = oldArray.getJSONObject(i);
            if (!containsIndependentFeatureId(newArray, oldTest.getString(Constants.FEATURE_ID_TOKEN))) {
                if (!olderPath.equals(targetDir)) {
                    copyFiles(oldParentPath, oldTest);
                }
                outputArray.put(oldTest);
                newObjects.add(oldTest.getString(Constants.FEATURE_ID_TOKEN));
            } else {
                replacedObjects.add(oldTest.getString(Constants.FEATURE_ID_TOKEN));
            }
        }
        try {
            System.out.println("Writing " + outputArray.length() + " engine independent tests.");
            Files.write(Paths.get(targetDir + File.separator + "merged-tests-engine-independent.json"), outputArray.toString(1).getBytes());
            System.out.println("done writing.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the array of engine independent tests contains the featureID
     *
     * @param testObjects of engine independent tests to be checked
     * @param featureId   to be checked
     * @return true when id is in the array, false otherwise
     */
    private boolean containsIndependentFeatureId(JSONArray testObjects, String featureId) {
        for (int i = 0; i < testObjects.length(); i++) {
            if (testObjects.getJSONObject(i).get(Constants.FEATURE_ID_TOKEN).equals(featureId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * merges the directories.
     */
    private void mergeDirectories() {
        List<File> oldFiles = Arrays.asList(new File(olderPath).listFiles(f -> f.getName().contains(".json")));
        File oldEngineDependent = null;
        File oldEngineIndependent = null;
        File oldConstructs = null;
        File oldEngines = null;
        for (File f : oldFiles) {
            if (f.getName().contains("tests-engine-dependent.json")) {
                oldEngineDependent = f;
            } else if (f.getName().contains("tests-engine-independent.json")) {
                oldEngineIndependent = f;
            } else if (f.getName().contains("constructs.json")) {
                oldConstructs = f;
            } else if (f.getName().contains("engines.json")) {
                oldEngines = f;
            }
        }

        List<File> newFiles = Arrays.asList(new File(newerPath).listFiles(f -> f.getName().contains(".json")));
        File newEngineDependent = null;
        File newEngineIndependent = null;
        File newConstructs = null;
        File newEngines = null;
        for (File f : newFiles) {
            if (f.getName().contains("tests-engine-dependent.json")) {
                newEngineDependent = f;
            } else if (f.getName().contains("tests-engine-independent.json")) {
                newEngineIndependent = f;
            } else if (f.getName().contains("constructs.json")) {
                newConstructs = f;
            } else if (f.getName().contains("engines.json")) {
                newEngines = f;
            }
        }
        if (oldEngineDependent != null && newEngineDependent != null) {
            new JsonMerger(oldEngineDependent.toString(), newEngineDependent.toString()).merge();
        }
        if (oldEngineIndependent != null && newEngineIndependent != null) {
            new JsonMerger(oldEngineIndependent.toString(), newEngineIndependent.toString()).merge();
        }
        if (oldConstructs != null && newConstructs != null) {
            new JsonMerger(oldConstructs.toString(), newConstructs.toString()).merge();
        }
        if (oldEngines != null && newEngines != null) {
            new JsonMerger(oldEngines.toString(), newEngines.toString()).merge();
        }
    }

    /**
     * merges tests-engine-dependent.json
     */
    private void mergeEngineDependent() {
        new PathConverter(olderPath, true).convert();
        new PathConverter(newerPath, true).convert();
        String oldLine = readLines(olderPath);
        String newLine = readLines(newerPath);
        JSONArray oldArray = new JSONArray(oldLine);
        JSONArray newArray = new JSONArray(newLine);
        System.out.println("Read " + oldArray.length() + " old engine dependent tests.");
        System.out.println("Read " + newArray.length() + " new engine dependent tests.");
        JSONArray outputArray = new JSONArray();
        for (int i = 0; i < newArray.length(); i++) {
            JSONObject newTest = newArray.getJSONObject(i);
            String newParentPath = new File(newerPath).getAbsoluteFile().getParent();
            if (!newerPath.equals(targetDir)) {
                copyFiles(newParentPath, newTest);
            }
            outputArray.put(newArray.get(i));
        }
        String oldParentPath = new File(olderPath).getAbsoluteFile().getParent();
        for (int i = 0; i < oldArray.length(); i++) {
            JSONObject oldTest = oldArray.getJSONObject(i);
            if (!containsDependentTest(newArray, oldTest.getString(Constants.FEATURE_ID_TOKEN), oldTest.getString(Constants.ENGINE_ID_TOKEN))) {
                copyFiles(oldParentPath, oldTest);
                outputArray.put(oldTest);
                newObjects.add(oldTest.getString(Constants.FEATURE_ID_TOKEN) + " of " + oldTest.getString(Constants.ENGINE_ID_TOKEN));
            } else {
                replacedObjects.add(oldTest.getString(Constants.FEATURE_ID_TOKEN) + " of " + oldTest.getString(Constants.ENGINE_ID_TOKEN));
            }
        }
        try {
            System.out.println("Writing " + outputArray.length() + " engine dependent tests.");
            Files.write(Paths.get(targetDir + File.separator + "merged-tests-engine-dependent.json"), outputArray.toString(1).getBytes());
            System.out.println("done writing.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the file as a line.
     *
     * @param filePath to be read
     * @return the contents of the file, or '[]' in case of an error.
     */
    private String readLines(String filePath) {
        String line = "[]";
        System.out.println("Reading: " + filePath + " ...");
        try {
            line = Files.readAllLines(Paths.get(filePath)).parallelStream()
                    .reduce((s1, s2) -> s1 + s2)
                    .orElse(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done Reading.");
        return line;
    }

    /**
     * Checks if the array of engine dependent tests contains a est with the featureId and engineId
     *
     * @param testObjects of the engine dependent tests to be checked
     * @param featureId   to be checked
     * @param engineId    to be checked
     * @return true when the test with featureId/engineId is in the array, false otherwise
     */
    private boolean containsDependentTest(JSONArray testObjects, String featureId, String engineId) {
        for (int i = 0; i < testObjects.length(); i++) {
            if (testObjects.getJSONObject(i).get(Constants.FEATURE_ID_TOKEN).equals(featureId) && testObjects.getJSONObject(i).get(Constants.ENGINE_ID_TOKEN).equals(engineId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * copies the files in the array from the oldParent to the targetDir.
     *
     * @param oldParent to be copied from
     * @param obj       to be copied
     */
    private void copyFiles(String oldParent, JSONObject obj) {
        List<String> toCopy = new ArrayList<>();
        if (obj.has(Constants.ENGINE_INDEPENDENT_FILES_TOKEN)) {
            JSONArray arr = obj.getJSONArray(Constants.ENGINE_INDEPENDENT_FILES_TOKEN);
            for (int i = 0; i < arr.length(); i++) {
                toCopy.add(arr.getString(i));
            }
        }
        if (obj.has(Constants.ENGINE_DEPENDENT_FILES_TOKEN)) {
            JSONArray arr = obj.getJSONArray(Constants.ENGINE_DEPENDENT_FILES_TOKEN);
            for (int i = 0; i < arr.length(); i++) {
                toCopy.add(arr.getString(i));
            }
        }
        if (obj.has(Constants.LOG_FILES_TOKEN)) {
            JSONArray arr = obj.getJSONArray(Constants.LOG_FILES_TOKEN);
            for (int i = 0; i < arr.length(); i++) {
                toCopy.add(arr.getString(i));
            }
        }
        if (toCopy.size() > 0) {
            toCopy.parallelStream().forEach(p -> {
                String newPath = targetDir + File.separator + p;
                String oldPath = oldParent + File.separator + p;
                new File(newPath).mkdirs();
                try {
                    Files.copy(Paths.get(oldPath), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
