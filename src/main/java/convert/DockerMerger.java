package convert;

import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Merges an docker run.
 */
public class DockerMerger {

    /**
     * the path from the run directory to the test-engine-dependent.json.
     */
    private static String TEST_DEPENDENT_PATH = File.separator + "test" + File.separator + "tests-engine-dependent.json";

    /**
     * root path of the docker run.
     */
    private Path rootPath;
    /**
     * List to store the skipped paths.
     */
    private final List<String> skipped = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param root path to be root of the docker run
     */
    public DockerMerger(String root) {
        rootPath = Paths.get(root);
    }

    /**
     * Merges the docker run.
     */
    public void merge() {
        try {
            List<Path> testFolders = Files.list(rootPath).filter(p -> p.toFile().isDirectory())
                    .collect(Collectors.toList());

            List<Path> testJsonPaths = testFolders.stream().map(p -> Paths.get(p.toString() + TEST_DEPENDENT_PATH))
                    .filter(p -> {
                        boolean result = p.toFile().exists();
                        if (!result) {
                            skipped.add(p.toString().substring(0, p.toString().lastIndexOf(File.separator)));
                        }
                        return result;
                    })
                    .collect(Collectors.toList());

            int counter = 1;
            Path targetFileDir = Paths.get(rootPath.toString() + File.separator + "files");

            JSONArray outputArray = new JSONArray();
            for (Path p : testJsonPaths) {
                System.out.println("MERGING " + counter + " of " + testJsonPaths.size());
                counter++;
                new PathConverter(p.toString(), true).convert();
                copyDirectory(Paths.get(p.toFile().getParent() + File.separator + "files"), targetFileDir);
                System.out.println("Reading: ");
                JSONArray arr = new JSONArray(readLines(p.toString()));
                for (int i = 0; i < arr.length(); i++) {
                    outputArray.put(arr.getJSONObject(i));
                }
            }
            Path rootJson = Paths.get(rootPath.toString() + File.separator + "tests-engine-dependent.json");
            Files.write(rootJson, outputArray.toString(1).getBytes());
            System.out.println("Skipped because no tests-engine-dependent.json exists:");
            skipped.forEach(System.out::println);
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
        String line = "";
        try {
            line = Files.readAllLines(Paths.get(filePath)).stream().reduce((s1, s2) -> s1 + s2).orElse("");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    /**
     * Copies the files directory of the docker test to the root
     *
     * @param sourceDir of the files directory
     * @param targetDir directory to copy to
     */
    private void copyDirectory(Path sourceDir, Path targetDir) {
        try {
            Files.walk(sourceDir).forEach(path -> {

                try {
                    Path target = Paths.get(path.toString().replace(
                            sourceDir.toString(),
                            targetDir.toString()));
                    if (!target.toFile().exists()) {
                        Files.copy(path, target);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
