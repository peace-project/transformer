package convert;

/**
 * Main entry point.
 * <ul>
 *     <li>merge oldPath newPath</li>
 *     <li>mergeDocker rootPath</li>
 *     <li>copyFiles filePath</li>
 *     <li>test directoryPath</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) {
        if (args.length <= 1) {
            System.out.println("Specify arguments first");
            return;
        }
        String type = args[0];
        if (type.equalsIgnoreCase("merge")) {
            new JsonMerger(args[1], args[2]).merge();
        } else if (type.equals("mergeDocker")) {
            new DockerMerger(args[1]).merge();
        } else if (type.equalsIgnoreCase("copyFiles")) {
            new PathConverter(args[1], true).convert();
        } else if (type.equalsIgnoreCase("test")) {
            new Tester(args[1]).test();
        } else {
            System.out.println(type + " is not a valid argument.");
        }

    }
}
