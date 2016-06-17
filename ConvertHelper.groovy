import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * @author David Bimamisa.
 */


if (args[0] == 'featureid') {
    updateFeatureID();
} else if (args[0] == 'filepath') {
    updateFilePath();
}

def updateFeatureID(){
    def cli = new CliBuilder(usage:'featureid [OPTION] ')

    cli.with {
        h longOpt: 'help', 'Show usage information'
        d longOpt: 'source-dependent', args: 1, argName: 'source-file', 'Source of engine-dependent JSON', required: true
        f longOpt: 'source-feature', args: 1, argName: 'source-file', 'Source of featureTree JSON', required: true
    }

    def options = cli.parse(args[1..args.length-1])
    if (!options || options.h) {
       // cli.usage()
        System.exit(1)
    }

    def testDependentFile =   'tests-engine-dependent_large.json';
    def featureTreeFile =   'feature-tree.json';

    def jsonSlurper = new JsonSlurper()
    def testDependent = jsonSlurper.parse(new File(testDependentFile))
    def featureTree = jsonSlurper.parse(new File(featureTreeFile))


    featureTree.find {  it.id.toString() == 'Expressiveness' }.languages.find{ it.name == 'BPMN'}.groups.each{ group ->
         group.constructs.each{ construct ->
             def wcp = construct.name.split("_");
            construct.features.each { feature ->
                //def fIdSeg = feature.id.split("__");
                def oldFeatureID = 'Expressiveness__BPMN__cfpatterns__' + wcp[0]+ '__' + feature.name;
                 println oldFeatureID + ' ==> ' +  feature.id
                testDependent.findAll{it.featureID == oldFeatureID }.each{ item -> item.featureID = feature.id }
             }
         }
     }


    println new File(testDependentFile).getAbsolutePath()

    save(testDependent, 'tests-engine-dependent_large_3.json')
}

def updateFilePath(){
    def cli = new CliBuilder(usage:'filepath [OPTION]')
    cli.with {
        h longOpt: 'help', 'Show usage information'
        s longOpt: 'source', args: 1, argName: 'source-file', 'Source folder with JSON files', required: true
        e longOpt: 'type', args: 1, argName: 'file-type', 'i=engine-independent or d=engine-dependent', required: true
        t longOpt: 'test', args: 1, argName: 'test-folder', 'Testfolder', required: true
        d longOpt: 'dest', args: 1, argName: 'dest-dir', 'Destination dir'
    }

    //-e "d"
    println args[1..args.length-1]
    def options = cli.parse(args[1..args.length-1])

    if (!options || options.h) {
       // cli.usage()
        System.exit(1)
    }

    def target = (options.d) ?  options.d : options.s
    def testFolder = options.t

    if(options.e == 'i'){
        def jsonSlurper = new JsonSlurper()
        jsonFile = jsonSlurper.parse(new File(options.s + '/tests-engine-independent.json'))
        jsonFile = updateEngineIndependentFilePath(jsonFile)
    }
    else if(options.e == 'd') {
        jsonFile = new File(options.s + '/tests-engine-dependent_large.json');
        def jsonSlurper = new JsonSlurper()
        jsonContent = jsonSlurper.parse(jsonFile)

        println 'Staring updating file paths of tests-engine-dependent_large.json'
        jsonContent = updateEngineDependentFilePath(jsonContent, testFolder, target)

        println 'Start saving JSON file in ' + target
        save(jsonContent, jsonFile)

    } else {
        cli.usage()
        System.exit(1);
    }


}

def updateEngineDependentFilePath(jsonFile, testFolder, target){
    //println (jsonFile instanceof ArrayList)

    jsonFile.each { item ->
        item.logFiles.eachWithIndex { file, index ->
            item.logFiles[index] = file.replaceAll('\\\\', '/').replaceAll('test/','tests/')

            File srcFile = new File(testFolder +'/' + file.replaceAll('test\\\\', ''))
            File targetFile = new File(target +  '/' + item.logFiles[index])

          //  println 'Copy ' + srcFile.getAbsolutePath() + ' ==> ' + targetFile.getAbsolutePath()


            final Path targetDir = targetFile.toPath().getParent();
            if (targetDir != null)
                Files.createDirectories(targetDir);

            //Files.copy(srcFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        item.engineDependentFiles.eachWithIndex { file, index ->
            println file.replaceAll(/test/, '')
            File srcFile = new File(testFolder +'/' + file.replaceAll('test\\\\', ''))

            item.engineDependentFiles[index] = file.replaceAll('\\\\', '/').replaceAll('test/','tests/')
            File targetFile = new File(target +  '/' + item.engineDependentFiles[index])

            //println 'Copy ' + srcFile.getAbsolutePath() + ' ==> ' + targetFile.getAbsolutePath()


            final Path targetDir = targetFile.toPath().getParent();
            if (targetDir != null)
                Files.createDirectories(targetDir);

          //  Files.copy(srcFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        }
    }

    return jsonFile;
}

def updateFilePath(file, testFolder, target){
    File srcFile = new File(testFolder +'/' + file.replaceAll('test\\\\', ''))

    File targetFile = new File(target +  '/' + file)

    println 'Copy ' + srcFile.getAbsolutePath() + ' ==> ' + targetFile.getAbsolutePath()

    final Path targetDir = targetFile.toPath().getParent();
    if (targetDir != null)
        Files.createDirectories(targetDir);

    Files.copy(srcFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
}

def updateEngineIndependentFilePath(jsonFile, testFolder){
    jsonFile.each{ item ->
        item.engineIndependentFiles.eachWithIndex { file, index ->
            item.engineIndependentFiles[index] = file.replaceAll('src\\\\main\\\\tests\\\\files\\\\', 'features/')
                    .replaceAll('\\\\', '/')

            //Files.copy(file, item.engineIndependentFiles[index]);

            //return file;

            //  tests/activiti__5_19_0/cfpatterns__WCP02_ParallelSplit/process/WCP02_ParallelSplit.bpmn

            // features/bpmn/gateways/ExclusiveGateway.bpmn

            //tests\\activiti__5_19_0\\cfpatterns__WCP02_ParallelSplit\\process\\WCP02_ParallelSplit.bpmn
            //src\\main\\tests\\files\\bpmn\\gateways\\ExclusiveGateway.bpmn
        }
    }
    return jsonFile;
}

static save(Object content, File targetFile) {
    targetFile.write(new JsonBuilder(content).toPrettyString())
}


