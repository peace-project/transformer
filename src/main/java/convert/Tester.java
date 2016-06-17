package convert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Test the integrity of the data.
 */
public class Tester {

    /**
     * Root director to be checked.
     */
    private final String rootDir;

    /**
     * Array of engines.json
     */
    private JSONArray enginesArr;

    /**
     * Array of feature-tree.json
     */
    private JSONArray featureTreeArr;

    /**
     * Array of tests-engine-dependent.json
     */
    private JSONArray engineDependentArr;

    /**
     * Array of tests-engine-independent.json
     */
    private JSONArray engineIndependentArr;

    /**
     * Constructor.
     *
     * @param rootDir to be checked
     */
    public Tester(String rootDir) {
        this.rootDir = rootDir;
    }

    /**
     * Tests the integrity.
     */
    public final void test() {
        System.out.println("testing: " + rootDir);
        String enginesPath = rootDir + File.separator + "engines.json";
        String featureTreePath = rootDir + File.separator + "feature-tree.json";
        String engineDependentPath = rootDir + File.separator + "tests-engine-dependent.json";
        String engineIndependentPath = rootDir + File.separator + "tests-engine-independent.json";
        System.out.println("Reading " + enginesPath);
        enginesArr = readFile(enginesPath);
        System.out.println("Done reading.");
        System.out.println("Reading " + featureTreePath);
        featureTreeArr = readFile(featureTreePath);
        System.out.println("Done reading.");
        System.out.println("Reading " + engineDependentPath);
        engineDependentArr = readFile(engineDependentPath);
        System.out.println("Done reading.");
        System.out.println("Reading " + engineIndependentPath);
        engineIndependentArr = readFile(engineIndependentPath);
        System.out.println("Done reading.");

        checkEngine();

        checkFeatureTree();

        checkEngineIndependent();

        checkDuplicateEngineDependent();

        checkEngineDependent();

    }

    /**
     * Checks the feature-tree
     */
    private void checkFeatureTree() {
        List<String> output = new ArrayList<>();
        output.add("Checking feature-tree.json...");
        for (int i = 0; i < featureTreeArr.length(); i++) {
            JSONObject capObj = featureTreeArr.getJSONObject(i);
            JSONArray langArr = capObj.getJSONArray("languages");
            if (langArr.length() == 0) {
                output.add("Capability " + capObj.getString("id") + " has no languages.");
            }
            //language
            for (int j = 0; j < langArr.length(); j++) {
                JSONObject langObj = langArr.getJSONObject(i);
                JSONArray groupArr = langObj.getJSONArray("groups");
                if (groupArr.length() == 0) {
                    output.add("Language " + capObj.getString("id") + " has no groups.");
                }
                //group
                for (int k = 0; k < groupArr.length(); k++) {
                    JSONObject groupObj = groupArr.getJSONObject(i);
                    JSONArray constructsArr = groupObj.getJSONArray("constructs");
                    if (constructsArr.length() == 0) {
                        output.add("Group " + groupObj.getString("id") + " has no groups.");
                    }
                    //constructs
                    for (int l = 0; l < constructsArr.length(); l++) {
                        JSONObject constructsObj = constructsArr.getJSONObject(i);
                        JSONArray featuresArr = constructsObj.getJSONArray("features");
                        if (featuresArr.length() == 0) {
                            output.add("Construct " + constructsObj.getString("id") + " has no groups.");
                        }
                        //feature
                        for (int m = 0; m < featuresArr.length(); m++) {
                            JSONObject featuresObj = featuresArr.getJSONObject(i);
                            String id = featuresObj.getString("id");
                            if (!checkEngineDependentForFeature(id)) {
                                output.add("Feature " + id + " has no tests in engine dependent");
                            }
                        }
                    }
                }
            }
            writeLines(output, rootDir + File.separator + "feature-tree-test.txt");
        }
    }

    /**
     * Checks the engine independent
     */
    private void checkEngineIndependent() {
        List<String> output = new ArrayList<>();
        output.add("Checking engineIndependent.json...");
        int counter = 0;
        for (int i = 0; i < engineIndependentArr.length(); i++) {
            JSONObject obj = engineIndependentArr.getJSONObject(i);
            String id = obj.getString("featureID");
            if (!checkEngineDependentForFeature(id)) {
                counter++;
                output.add("Feature is not in engineDependent: " + id);
            }
            if (!checkFeatureTreeForFeature(id)) {
                counter++;
                output.add("Feature is not in the feature-tree: " + id);
            }
        }
        output.add("Found " + counter + " test declarations without tests.");
        writeLines(output, rootDir + File.separator + "tests-engine-independent-test.txt");
    }

    /**
     * Checks the engine dependent.
     */
    private void checkEngineDependent() {
        List<String> output = new ArrayList<>();
        output.add("Checking engineDependent.json...");
        int counter = 0;
        for (int i = 0; i < engineDependentArr.length(); i++) {
            JSONObject obj = engineDependentArr.getJSONObject(i);
            String id = obj.getString("engineID");
            if (!checkEngineForEngine(id)) {
                counter++;
                output.add("No engine declaration for test: " + obj.getString("featureID") + "(" + id + ")");
            }
        }
        output.add("Found " + counter + " tests without engine declarations.");
        writeLines(output, rootDir + File.separator + "tests-engine-dependent-test.txt");
    }

    private void checkDuplicateEngineDependent() {
        List<String> output = new ArrayList<>();
        output.add("Checking tests-engine-dependent.json for duplicates");
        int counter = 0;
        for (int i = 0; i < engineDependentArr.length(); i++) {
            for (int j = i + 1; j < engineDependentArr.length(); j++) {
                String featureIdI = engineDependentArr.getJSONObject(i).getString(Constants.FEATURE_ID_TOKEN);
                String featureIdJ = engineDependentArr.getJSONObject(j).getString(Constants.FEATURE_ID_TOKEN);
                String engineIdI = engineDependentArr.getJSONObject(i).getString(Constants.ENGINE_ID_TOKEN);
                String engineIdJ = engineDependentArr.getJSONObject(j).getString(Constants.ENGINE_ID_TOKEN);
                if (featureIdI.equals(featureIdJ) && engineIdI.equals(engineIdJ)) {
                    counter++;
                    output.add("duplicate " + featureIdI + " on engine " + engineIdI);
                }
            }
        }
        output.add("Found "+ counter+" duplicate tests.");
        writeLines(output, rootDir + File.separator + "tests-engine-dependent-duplicate-test.txt");
    }

    /**
     * Checks the engines.
     */
    private void checkEngine() {
        List<String> output = new ArrayList<>();
        output.add("Checking engines.json...");
        int counter = 0;
        for (int i = 0; i < enginesArr.length(); i++) {
            JSONObject obj = enginesArr.getJSONObject(i);
            String id = obj.getString("id");
            if (!checkEngineDependentForEngine(id)) {
                counter++;
                output.add("No tests for engine: " + id);
            }
        }
        output.add("Found " + counter + " unused engines.");
        writeLines(output, rootDir + File.separator + "engines-test.txt");
    }

    /**
     * Checks if the engine dependent contains the engineId
     *
     * @param engineId to be checked
     * @return true if the {@link #engineDependentArr} contains the engineId, false otherwise
     */
    private boolean checkEngineDependentForEngine(String engineId) {
        for (int i = 0; i < engineDependentArr.length(); i++) {
            String id = engineDependentArr.getJSONObject(i).getString("engineID");
            if (id.equals(engineId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the engines contains the engineId
     *
     * @param engineId to be checked
     * @return true if the {@link #enginesArr} contains the engineId, false otherwise
     */
    private boolean checkEngineForEngine(String engineId) {
        for (int i = 0; i < enginesArr.length(); i++) {
            if (enginesArr.getJSONObject(i).getString("id").equals(engineId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the engine independent contains the featureId
     *
     * @param featureId to be checked
     * @return true if the {@link #engineIndependentArr} contains the featureId, false otherwise
     */
    private boolean checkEngineDependentForFeature(String featureId) {
        for (int i = 0; i < engineDependentArr.length(); i++) {
            if (engineDependentArr.getJSONObject(i).getString("featureID").equals(featureId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the feature tree contains the featureId.
     *
     * @param featureId to be checked
     * @return true if the {@link #featureTreeArr} contains the engineId, false otherwise
     */
    private boolean checkFeatureTreeForFeature(String featureId) {
        for (int i = 0; i < featureTreeArr.length(); i++) {
            JSONArray langArr = featureTreeArr.getJSONObject(i).getJSONArray("languages");
            //language
            for (int j = 0; j < langArr.length(); j++) {
                JSONArray groupArr = langArr.getJSONObject(j).getJSONArray("groups");
                //group
                for (int k = 0; k < groupArr.length(); k++) {
                    JSONArray constructsArr = groupArr.getJSONObject(k).getJSONArray("constructs");
                    //constructs
                    for (int l = 0; l < constructsArr.length(); l++) {
                        JSONArray featuresArr = constructsArr.getJSONObject(l).getJSONArray("features");
                        //feature
                        for (int m = 0; m < featuresArr.length(); m++) {
                            String id = featuresArr.getJSONObject(m).getString("id");
                            if (id.equals(featureId)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Reads the file.
     *
     * @param path to be read
     * @return the JSONArray of the file
     */
    private JSONArray readFile(String path) {
        String source = readLines(path);
        if (source == null || source == "") {
            return new JSONArray();
        }
        return new JSONArray(source);
    }

    /**
     * Reads the file as a line.
     *
     * @param filePath to be read
     * @return the contents of the file, or '[]' in case of an error.
     */
    private String readLines(String filePath) {
        String line = "[]";
        try {
            line = Files.readAllLines(Paths.get(filePath)).parallelStream()
                    .reduce((s1, s2) -> s1 + s2)
                    .orElse(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    private void writeLines(List<String> lines, String filePath) {
        lines.forEach(System.out::println);
        String line = lines.stream().reduce((a, b) -> a + "\n" + b).get();
        try {
            Files.write(Paths.get(filePath), line.getBytes());
        } catch (IOException e) {
            System.err.println("Error while writing file: " + e.getMessage());
        }
    }
}